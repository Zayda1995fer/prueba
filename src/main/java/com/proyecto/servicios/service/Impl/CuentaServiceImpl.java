package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.exception.CuentaNoEncontradaException;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.CuentaService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@Slf4j
public class CuentaServiceImpl implements CuentaService {

    private final CuentaRepository cuentaRepository;

    public CuentaServiceImpl(CuentaRepository cuentaRepository) {
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public CuentaResponse consultarPorNumeroCuenta(String numeroCuenta) {
        Cuenta cuenta = obtenerCuentaOLanzar(numeroCuenta);
        return ClienteServiceImpl.mapearCuenta(cuenta);
    }

    @Override
    public List<CuentaResponse> consultarActivas() {
        return cuentaRepository.findByEstatus(Cuenta.ESTATUS_ACTIVA).stream()
                .map(ClienteServiceImpl::mapearCuenta)
                .toList();
    }

    @Override
    public BigDecimal consultarSaldo(String numeroCuenta) {
        return obtenerCuentaOLanzar(numeroCuenta).getSaldo();
    }

    private Cuenta obtenerCuentaOLanzar(String numeroCuenta) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new CuentaNoEncontradaException(numeroCuenta));
    }
}