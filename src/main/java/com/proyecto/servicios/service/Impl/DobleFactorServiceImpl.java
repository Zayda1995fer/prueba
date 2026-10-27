package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Codigo2fa;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.onboarding.Codigo2faRequest;
import com.proyecto.servicios.model.onboarding.Codigo2faResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.Codigo2faRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.CifradoService;
import com.proyecto.servicios.service.DobleFactorService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDateTime;

@Slf4j
@Service
public class DobleFactorServiceImpl implements DobleFactorService {

    private static final String TM = "sfTransactionManager";

    private final Codigo2faRepository codigoRepository;
    private final CuentaRepository cuentaRepository;
    private final ClienteRepository clienteRepository;
    private final CifradoService cifradoService;
    private final SecureRandom aleatorio = new SecureRandom();

    @Value("${app.pagos.2fa.vigencia-minutos:5}")
    private int vigenciaMinutos;

    @Value("${app.pagos.2fa.max-intentos:3}")
    private int maxIntentos;

    @Value("${app.pagos.2fa.devolver-codigo:false}")
    private boolean devolverCodigo;

    public DobleFactorServiceImpl(Codigo2faRepository codigoRepository, CuentaRepository cuentaRepository,
                                  ClienteRepository clienteRepository, CifradoService cifradoService) {
        this.codigoRepository = codigoRepository;
        this.cuentaRepository = cuentaRepository;
        this.clienteRepository = clienteRepository;
        this.cifradoService = cifradoService;
    }

    @Override
    @Transactional(transactionManager = TM)
    public Codigo2faResponse solicitar(Codigo2faRequest request) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(request.getNumeroCuenta())
                .orElseThrow(() -> new NegocioException(CatalogoError.CUENTA_NO_ENCONTRADA, request.getNumeroCuenta()));
        if (!cuenta.getClienteId().equals(request.getIdUser())) {
            throw new NegocioException(CatalogoError.CUENTA_NO_PERTENECE_AL_CLIENTE);
        }
        Cliente cliente = clienteRepository.findById(request.getIdUser())
                .orElseThrow(() -> new NegocioException(CatalogoError.CLIENTE_NO_ENCONTRADO, request.getIdUser()));
        if (!Boolean.TRUE.equals(cliente.getActivo()) || !Cuenta.ESTATUS_ACTIVA.equals(cuenta.getEstatus())) {
            throw new NegocioException(CatalogoError.PAGO_CLIENTE_INACTIVO);
        }

        // Un solo código vigente a la vez
        for (Codigo2fa anterior : codigoRepository.findByClienteIdAndCuentaIdAndUsadoFalse(cliente.getId(), cuenta.getId())) {
            anterior.setUsado(true);
            codigoRepository.save(anterior);
        }

        String codigo = String.format("%06d", aleatorio.nextInt(1_000_000));
        Codigo2fa nuevo = new Codigo2fa();
        nuevo.setClienteId(cliente.getId());
        nuevo.setCuentaId(cuenta.getId());
        nuevo.setCodigoHash(cifradoService.hashParaBusqueda(codigo));
        nuevo.setFechaExpiracion(LocalDateTime.now().plusMinutes(vigenciaMinutos));
        nuevo.setIntentos(0);
        nuevo.setUsado(false);
        codigoRepository.save(nuevo);

        log.info("Código 2FA generado para cliente={}", cliente.getId());
        Codigo2faResponse response = new Codigo2faResponse();
        response.setVigenciaMinutos(vigenciaMinutos);
        response.setFechaExpiracion(nuevo.getFechaExpiracion());
        if (devolverCodigo) {
            response.setCodigo(codigo);
        }
        return response;
    }

    /**
     * Transacción propia: si el pago se revierte, el conteo de intentos fallidos
     * del código sí se conserva (si no, se podría adivinar el código sin límite).
     */
    @Override
    @Transactional(transactionManager = TM, propagation = Propagation.REQUIRES_NEW,
            noRollbackFor = NegocioException.class)
    public void validar(Integer clienteId, Integer cuentaId, String codigo) {
        Codigo2fa vigente = codigoRepository
                .findFirstByClienteIdAndCuentaIdAndUsadoFalseOrderByFechaCreacionDesc(clienteId, cuentaId)
                .orElseThrow(() -> new NegocioException(CatalogoError.DOBLE_FACTOR_NO_SOLICITADO));

        if (vigente.getFechaExpiracion().isBefore(LocalDateTime.now())) {
            vigente.setUsado(true);
            codigoRepository.save(vigente);
            throw new NegocioException(CatalogoError.DOBLE_FACTOR_EXPIRADO);
        }
        if (vigente.getIntentos() >= maxIntentos) {
            throw new NegocioException(CatalogoError.DOBLE_FACTOR_BLOQUEADO);
        }
        byte[] recibido = String.valueOf(cifradoService.hashParaBusqueda(codigo)).getBytes(StandardCharsets.UTF_8);
        byte[] esperado = vigente.getCodigoHash().getBytes(StandardCharsets.UTF_8);
        if (!MessageDigest.isEqual(recibido, esperado)) {
            vigente.setIntentos(vigente.getIntentos() + 1);
            codigoRepository.save(vigente);
            log.warn("Código 2FA incorrecto para cliente={}, intento {}", clienteId, vigente.getIntentos());
            throw new NegocioException(CatalogoError.DOBLE_FACTOR_INVALIDO);
        }
        vigente.setUsado(true);
        codigoRepository.save(vigente);
    }
}
