package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.CuentaResponse;

import java.math.BigDecimal;
import java.util.List;

public interface CuentaService {

    CuentaResponse consultarPorNumeroCuenta(String numeroCuenta);

    List<CuentaResponse> consultarActivas();

    BigDecimal consultarSaldo(String numeroCuenta);
}