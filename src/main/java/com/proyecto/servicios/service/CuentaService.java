package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.CuentaResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CuentaService {

    CuentaResponse consultarPorNumeroCuenta(String numeroCuenta, Integer clienteAutenticado);

    List<CuentaResponse> consultarActivas(Integer clienteAutenticado);

    BigDecimal consultarSaldo(String numeroCuenta, Integer clienteAutenticado);
}
