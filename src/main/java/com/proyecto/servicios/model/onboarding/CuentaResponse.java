package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
public class CuentaResponse {
    // Solo viajan al crear la cuenta; en las consultas van en null y no se muestran.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer id;
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer clienteId;
    private String numeroCuenta;
    // Siempre con 2 decimales y como texto ("0.00")
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "0.00")
    private BigDecimal saldo;
    private String estatus;
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(type = "string", example = "2026-10-09 18:16:05", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaApertura;

    public void setSaldo(BigDecimal saldo) {
        this.saldo = saldo == null ? null : saldo.setScale(2, RoundingMode.HALF_UP);
    }
}