package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;


@Getter
@Setter
@NoArgsConstructor
public class ClienteResponse {

    // Solo viaja al dar de alta o actualizar; en las consultas va en null y no se muestra.
    @JsonInclude(JsonInclude.Include.NON_NULL)
    private Integer id;
    private String nombre;
    private String segundoNombre;
    private String apellidoPaterno;
    private String apellidoMaterno;
    private LocalDate fechaNacimiento;
    private String curp;
    private String rfc;
    private String sexo;
    // Arreglo con un objeto { "id": ..., "nombre": ... }
    private List<NacionalidadResponse> nacionalidad;
    private String estadoCivil;
    private String correoElectronico;
    private Long telefonoMovil;
    private Long telefonoAlternativo;
    private String ocupacion;
    private String empresa;
    // Siempre con 2 decimales y como texto ("25000.00"), para que no se pierda el .00
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    @Schema(type = "string", example = "25000.00")
    private BigDecimal ingresoMensual;
    private Boolean activo;
    // Solo lectura: la genera el sistema y no se puede enviar ni editar.
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    @Schema(type = "string", example = "2026-10-09 18:16:05", accessMode = Schema.AccessMode.READ_ONLY)
    private LocalDateTime fechaCreacion;

    private DomicilioResponse domicilio;
    private CuentaResponse cuenta;

    public void setIngresoMensual(BigDecimal ingresoMensual) {
        this.ingresoMensual = ingresoMensual == null ? null : ingresoMensual.setScale(2, RoundingMode.HALF_UP);
    }
}