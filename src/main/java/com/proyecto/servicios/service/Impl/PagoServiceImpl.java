package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago;
import com.proyecto.servicios.model.onboarding.PagoConsultaRequest;
import com.proyecto.servicios.model.onboarding.PagoRequest;
import com.proyecto.servicios.model.onboarding.PagoResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.PagoRepository;
import com.proyecto.servicios.repositorys.onboarding.ProductoPagoRepository;
import com.proyecto.servicios.service.GestoPagoGateway;
import com.proyecto.servicios.service.PagoService;
import com.proyecto.servicios.service.PagoTransaccionService;
import com.proyecto.servicios.service.PagoTransaccionService.Reserva;
import com.proyecto.servicios.validation.ValidadorPago;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orquesta el pago. Esta clase NO es transaccional a propósito:
 *   1) valida contra el catálogo,
 *   2) reserva el saldo (transacción corta),
 *   3) llama a GestoPago (sin transacción ni bloqueos abiertos),
 *   4) registra el resultado (transacción corta).
 * Así un proveedor lento nunca deja la cuenta bloqueada, y un fallo entre el paso 2 y el 4
 * lo resuelve la conciliación: el pago queda PENDIENTE con el saldo retenido.
 */
@Slf4j
@Service
public class PagoServiceImpl implements PagoService {

    private final ValidadorPago validador;
    private final PagoTransaccionService transacciones;
    private final GestoPagoGateway gateway;
    private final PagoRepository pagoRepository;
    private final ProductoPagoRepository productoRepository;
    private final CuentaRepository cuentaRepository;

    public PagoServiceImpl(ValidadorPago validador, PagoTransaccionService transacciones, GestoPagoGateway gateway,
                           PagoRepository pagoRepository, ProductoPagoRepository productoRepository,
                           CuentaRepository cuentaRepository) {
        this.validador = validador;
        this.transacciones = transacciones;
        this.gateway = gateway;
        this.pagoRepository = pagoRepository;
        this.productoRepository = productoRepository;
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public PagoResponse pagar(PagoRequest request) {
        ProductoPago producto = validador.validar(request);

        Reserva reserva = transacciones.reservar(request, producto);
        Pago pago = reserva.pago();

        if (reserva.repeticion()) {
            return respuestaDePagoExistente(pago, producto);
        }

        ResultadoGestoPago resultado = gateway.enviarPago(pago, producto);
        pago = transacciones.registrarResultado(pago.getId(), resultado, false);

        if (Pago.ESTATUS_RECHAZADO.equals(pago.getEstatus())) {
            throw errorDeRechazo(pago, resultado);
        }
        log.info("Pago={} terminó con estatus {}", pago.getId(), pago.getEstatus());
        return armarRespuesta(pago, producto);
    }

    @Override
    public PagoResponse consultarPago(PagoConsultaRequest request) {
        Pago pago = pagoRepository.findByIdSolicitud(request.getIdSolicitud().toLowerCase())
                .filter(p -> p.getClienteId().equals(request.getIdUser()))
                // Mismo error si no existe o es de otro cliente: no se revela qué solicitudes existen
                .orElseThrow(() -> new NegocioException(CatalogoError.PAGO_NO_ENCONTRADO));
        ProductoPago producto = productoRepository.findById(pago.getIdProducto()).orElseThrow(
                () -> new NegocioException(CatalogoError.PRODUCTO_PAGO_NO_ENCONTRADO, pago.getIdProducto()));
        return armarRespuesta(pago, producto);
    }

    // Solicitud repetida: se responde con lo que ya pasó, sin volver a cobrar
    private PagoResponse respuestaDePagoExistente(Pago pago, ProductoPago producto) {
        if (Pago.ESTATUS_RECHAZADO.equals(pago.getEstatus())) {
            throw new NegocioException(CatalogoError.PAGO_RECHAZADO, pago.getCodigoRespuesta());
        }
        return armarRespuesta(pago, producto);
    }

    private NegocioException errorDeRechazo(Pago pago, ResultadoGestoPago resultado) {
        boolean propio = resultado.getTipo() == ResultadoGestoPago.Tipo.RECHAZADO && resultado.getError() != null;
        CatalogoError error = propio ? resultado.getError() : CatalogoError.PAGO_RECHAZADO;
        return error == CatalogoError.PAGO_RECHAZADO
                ? new NegocioException(error, pago.getCodigoRespuesta())
                : new NegocioException(error);
    }

    private PagoResponse armarRespuesta(Pago pago, ProductoPago producto) {
        // El saldo se lee al momento de responder: refleja el descuento (o el reintegro) ya confirmado
        Cuenta cuenta = cuentaRepository.findById(pago.getCuentaId())
                .orElseThrow(() -> new NegocioException(CatalogoError.CUENTA_NO_ENCONTRADA, pago.getCuentaId()));
        PagoResponse response = new PagoResponse();
        response.setIdSolicitud(pago.getIdSolicitud());
        // PENDIENTE solo existe en medio del flujo: para el cliente es lo mismo que "por conciliar"
        response.setEstatus(Pago.ESTATUS_PENDIENTE.equals(pago.getEstatus())
                ? Pago.ESTATUS_POR_CONCILIAR : pago.getEstatus());
        response.setFolioTransaccion(pago.getFolioTransaccion());
        response.setIdServicio(pago.getIdServicio());
        response.setIdProducto(producto.getIdProducto());
        response.setProducto(producto.getNombre());
        response.setMonto(pago.getMonto());
        response.setTipoDestino(producto.getTipoDestino());
        response.setDestino(ValidadorPago.enmascarar(pago.getDestino()));
        response.setSaldoActual(cuenta.getSaldo());
        response.setFechaPago(pago.getFechaCreacion());
        return response;
    }
}
