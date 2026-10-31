package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.entity.onboarding.Cuenta;
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
    public CuentaResponse consultarPorNumeroCuenta(String numeroCuenta, Integer clienteAutenticado) {
        Cuenta cuenta = obtenerCuentaDelCliente(numeroCuenta, clienteAutenticado);
        return ClienteServiceImpl.mapearCuenta(cuenta);
    }

    @Override
    public List<CuentaResponse> consultarActivas(Integer clienteAutenticado) {
        return cuentaRepository.findByClienteIdAndEstatus(clienteAutenticado, Cuenta.ESTATUS_ACTIVA).stream()
                .map(ClienteServiceImpl::mapearCuenta)
                .toList();
    }

    @Override
    public BigDecimal consultarSaldo(String numeroCuenta, Integer clienteAutenticado) {
        return obtenerCuentaDelCliente(numeroCuenta, clienteAutenticado).getSaldo();
    }

    // Mismo error si no existe o es de otro cliente: no se revela qué números de cuenta existen
    private Cuenta obtenerCuentaDelCliente(String numeroCuenta, Integer clienteAutenticado) {
        return cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .filter(c -> c.getClienteId().equals(clienteAutenticado))
                .orElseThrow(() -> new NegocioException(CatalogoError.CUENTA_NO_ENCONTRADA, numeroCuenta));
    }
}
