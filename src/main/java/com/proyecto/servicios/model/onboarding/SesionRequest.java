package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import static com.proyecto.servicios.validation.ReglasValidacion.TOKEN_SESION;

/** Token de sesión a validar; en el body para que no quede en la URL ni en logs de acceso. */
@Getter
@Setter
public class SesionRequest {

    @NotNull(message = "El token de sesión es obligatorio")
    @Pattern(regexp = TOKEN_SESION, message = "El token de sesión no tiene un formato válido")
    private String tokenSesion;
}
