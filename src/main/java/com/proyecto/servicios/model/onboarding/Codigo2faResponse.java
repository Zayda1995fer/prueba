package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Codigo2faResponse {

    /** Solo se devuelve en modo práctica (app.pagos.2fa.devolver-codigo=true); en producción se enviaría por otro canal. */
    private String codigo;
    private Integer vigenciaMinutos;
    private LocalDateTime fechaExpiracion;
}
