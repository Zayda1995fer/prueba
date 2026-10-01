package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CuentaResponse {
    private Integer id;
    private Integer clienteId;
    private String numeroCuenta;
    private BigDecimal saldo;
    private String estatus;
    private LocalDateTime fechaApertura;
}