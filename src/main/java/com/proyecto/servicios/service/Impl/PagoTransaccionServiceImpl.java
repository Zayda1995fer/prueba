package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago;
import com.proyecto.servicios.model.onboarding.PagoRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.PagoRepository;
import com.proyecto.servicios.repositorys.onboarding.ProductoPagoRepository;
import com.proyecto.servicios.service.DobleFactorService;
import com.proyecto.servicios.service.NotificacionService;
import com.proyecto.servicios.service.PagoTransaccionService;
import com.proyecto.servicios.validation.ValidadorPago;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
public class PagoTransaccionServiceImpl implements PagoTransaccionService {

    private static final String TM = "sfTransactionManager";

    private final ClienteRepository clienteRepository;
    private final CuentaRepository cuentaRepository;
    private final PagoRepository pagoRepository;
    private final ProductoPagoRepository productoRepository;
    private final DobleFactorService dobleFactorService;
    private final NotificacionService notificacionService;

    /** Ventana (segundos) en la que un pago idéntico se considera duplicado. */
    @Value("${app.pagos.ventana-duplicados-segundos:60}")
    private int ventanaDuplicadosSegundos;

    /** Intentos de consulta al proveedor antes de pasar el pago a revisión manual. */
    @Value("${app.pagos.conciliacion.max-intentos:10}")
    private int maxIntentosConciliacion;

    public PagoTransaccionServiceImpl(ClienteRepository clienteRepository, CuentaRepository cuentaRepository,
                                      PagoRepository pagoRepository, ProductoPagoRepository productoRepository,
                                      DobleFactorService dobleFactorService, NotificacionService notificacionService) {
        this.clienteRepository = clienteRepository;
        this.cuentaRepository = cuentaRepository;
        this.pagoRepository = pagoRepository;
        this.productoRepository = productoRepository;
        this.dobleFactorService = dobleFactorService;
        this.notificacionService = notificacionService;
    }

    // ------------------------------------------------------------------
    // Paso 1: reservar (descontar el saldo y registrar el pago)
    // ------------------------------------------------------------------

    @Override
    @Transactional(transactionManager = TM)
    public Reserva reservar(PagoRequest request, ProductoPago producto) {
        // El bloqueo de la cuenta serializa los pagos simultáneos: el segundo espera y ve el saldo ya descontado
        Cuenta cuenta = cuentaRepository.buscarParaPago(request.getNumeroCuenta())
                .orElseThrow(() -> new NegocioException(CatalogoError.CUENTA_NO_ENCONTRADA, request.getNumeroCuenta()));

        String destino = ValidadorPago.destinoDe(request);
        BigDecimal monto = request.getMonto().setScale(2, RoundingMode.HALF_UP);
        String huella = huella(cuenta.getId(), producto, destino, monto);

        // Idempotencia: la misma solicitud devuelve el pago original y NUNCA cobra otra vez
        String idSolicitud = request.getIdSolicitud().toLowerCase();
        var previo = pagoRepository.findByIdSolicitud(idSolicitud);
        if (previo.isPresent()) {
            Pago existente = previo.get();
            if (!existente.getClienteId().equals(request.getIdUser()) || !existente.getHuellaSolicitud().equals(huella)) {
                throw new NegocioException(CatalogoError.IDEMPOTENCIA_CONFLICTO);
            }
            log.info("Solicitud repetida, se devuelve el pago={} sin cobrar de nuevo", existente.getId());
            return new Reserva(existente, true);
        }

        if (!cuenta.getClienteId().equals(request.getIdUser())) {
            throw new NegocioException(CatalogoError.CUENTA_NO_PERTENECE_AL_CLIENTE);
        }
        Cliente cliente = clienteRepository.findById(request.getIdUser())
                .orElseThrow(() -> new NegocioException(CatalogoError.CLIENTE_NO_ENCONTRADO, request.getIdUser()));
        if (!Boolean.TRUE.equals(cliente.getActivo()) || !Cuenta.ESTATUS_ACTIVA.equals(cuenta.getEstatus())) {
            throw new NegocioException(CatalogoError.PAGO_CLIENTE_INACTIVO);
        }
        if (cuenta.getSaldo().compareTo(monto) < 0) {
            throw new NegocioException(CatalogoError.SALDO_INSUFICIENTE);
        }

        // Pago idéntico reciente (aunque traiga otro id de solicitud): se rechaza antes de gastar el código 2FA
        long similares = pagoRepository.contarSimilares(cuenta.getId(), producto.getIdProducto(), destino, monto,
                List.of(Pago.ESTATUS_PENDIENTE, Pago.ESTATUS_POR_CONCILIAR, Pago.ESTATUS_REVISION_MANUAL,
                        Pago.ESTATUS_EXITOSO),
                LocalDateTime.now().minusSeconds(ventanaDuplicadosSegundos));
        if (similares > 0) {
            throw new NegocioException(CatalogoError.PAGO_DUPLICADO);
        }

        dobleFactorService.validar(cliente.getId(), cuenta.getId(), request.getCodigo2fa());

        cuenta.setSaldo(cuenta.getSaldo().subtract(monto));
        cuentaRepository.save(cuenta);

        Pago pago = new Pago();
        pago.setIdSolicitud(idSolicitud);
        pago.setReferenciaProveedor(UUID.randomUUID().toString().replace("-", ""));
        pago.setHuellaSolicitud(huella);
        pago.setCuentaId(cuenta.getId());
        pago.setClienteId(cliente.getId());
        pago.setIdProducto(producto.getIdProducto());
        pago.setIdServicio(producto.getIdServicio());
        pago.setMonto(monto);
        pago.setDestino(destino);
        pago.setEstatus(Pago.ESTATUS_PENDIENTE);
        pago = pagoRepository.save(pago);

        log.info("Saldo reservado para el pago={}, producto={}", pago.getId(), producto.getIdProducto());
        return new Reserva(pago, false);
    }

    // ------------------------------------------------------------------
    // Paso 2: aplicar el resultado del proveedor
    // ------------------------------------------------------------------

    @Override
    @Transactional(transactionManager = TM)
    public Pago registrarResultado(Integer pagoId, ResultadoGestoPago resultado, boolean desdeConciliacion) {
        Pago pago = pagoRepository.buscarParaActualizar(pagoId)
                .orElseThrow(() -> new NegocioException(CatalogoError.PAGO_NO_ENCONTRADO));

        // Ya tiene resultado final (otra solicitud o la conciliación llegó primero): no se toca de nuevo
        if (!pago.enCurso()) {
            log.info("El pago={} ya estaba resuelto ({}), no se modifica", pago.getId(), pago.getEstatus());
            return pago;
        }

        switch (resultado.getTipo()) {
            case EXITOSO -> confirmar(pago, resultado);
            case RECHAZADO -> rechazar(pago, resultado);
            default -> dejarPorConciliar(pago, resultado, desdeConciliacion);
        }
        return pagoRepository.save(pago);
    }

    private void confirmar(Pago pago, ResultadoGestoPago resultado) {
        pago.setEstatus(Pago.ESTATUS_EXITOSO);
        pago.setFolioTransaccion(resultado.getFolio());
        pago.setCodigoRespuesta(resultado.getCodigo());
        log.info("Pago={} confirmado por el proveedor", pago.getId());
        avisarAlCliente(pago);
    }

    private void rechazar(Pago pago, ResultadoGestoPago resultado) {
        // El reintegro se hace una sola vez, con la cuenta bloqueada
        if (!Boolean.TRUE.equals(pago.getSaldoReintegrado())) {
            Cuenta cuenta = cuentaRepository.buscarPorIdParaActualizar(pago.getCuentaId())
                    .orElseThrow(() -> new NegocioException(CatalogoError.CUENTA_NO_ENCONTRADA, pago.getCuentaId()));
            cuenta.setSaldo(cuenta.getSaldo().add(pago.getMonto()));
            cuentaRepository.save(cuenta);
            pago.setSaldoReintegrado(true);
        }
        pago.setEstatus(Pago.ESTATUS_RECHAZADO);
        pago.setCodigoRespuesta(resultado.getCodigo());
        log.info("Pago={} rechazado por el proveedor ({}), saldo reintegrado", pago.getId(), resultado.getCodigo());
    }

    private void dejarPorConciliar(Pago pago, ResultadoGestoPago resultado, boolean desdeConciliacion) {
        pago.setCodigoRespuesta(resultado.getCodigo());
        if (desdeConciliacion) {
            pago.setIntentosConciliacion(pago.getIntentosConciliacion() + 1);
        }
        if (pago.getIntentosConciliacion() >= maxIntentosConciliacion) {
            // Se deja de insistir: el saldo sigue retenido y una persona debe revisarlo con el proveedor
            pago.setEstatus(Pago.ESTATUS_REVISION_MANUAL);
            log.error("Pago={} sin resolver tras {} consultas: requiere revisión manual",
                    pago.getId(), pago.getIntentosConciliacion());
        } else {
            pago.setEstatus(Pago.ESTATUS_POR_CONCILIAR);
            log.warn("Pago={} sin resultado definitivo ({}): saldo retenido, queda por conciliar",
                    pago.getId(), resultado.getCodigo());
        }
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    // El correo sale hasta que el pago quedó confirmado en la BD (después del commit)
    private void avisarAlCliente(Pago pago) {
        Runnable envio = () -> {
            try {
                Cliente cliente = clienteRepository.findById(pago.getClienteId()).orElse(null);
                Cuenta cuenta = cuentaRepository.findById(pago.getCuentaId()).orElse(null);
                ProductoPago producto = productoRepository.findById(pago.getIdProducto()).orElse(null);
                if (cliente == null || cuenta == null || producto == null) {
                    return;
                }
                notificacionService.notificarPago(cliente.getCorreoElectronico(), cliente.getNombre(),
                        cuenta.getNumeroCuenta(), producto.getNombre(), pago.getMonto(), cuenta.getSaldo(),
                        pago.getFolioTransaccion(),
                        pago.getFechaCreacion() != null ? pago.getFechaCreacion() : LocalDateTime.now());
            } catch (Exception e) {
                log.error("No fue posible preparar el aviso del pago={} ({})", pago.getId(), e.getClass().getSimpleName());
            }
        };
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    envio.run();
                }
            });
        } else {
            envio.run();
        }
    }

    /** Huella de lo que se está pagando: detecta que un mismo id de solicitud se reutilice con otros datos. */
    private String huella(Integer cuentaId, ProductoPago producto, String destino, BigDecimal monto) {
        String base = cuentaId + "|" + producto.getIdServicio() + "|" + producto.getIdProducto() + "|" + destino
                + "|" + monto.toPlainString();
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(base.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 no disponible", e);
        }
    }
}
