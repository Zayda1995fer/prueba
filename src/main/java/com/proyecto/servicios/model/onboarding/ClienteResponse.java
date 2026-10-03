package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Lo que el API le regresa al cliente que consulta. Incluye el domicilio
 * y la cuenta ya "armados" para no obligar a quien consuma el API a
 * hacer dos llamadas extra.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteResponse {

    private Integer id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String sexo;
    private String nacionalidad;
    private String estadoCivil;
    private String correoElectronico;
    private Long telefonoMovil;
    private Long telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    private BigDecimal ingresoMensual;
    private Boolean activo;
    private LocalDateTime fechaCreacion;

    private DomicilioResponse domicilio;
    private CuentaResponse cuenta;
}