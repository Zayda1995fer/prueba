package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** Activa o desactiva el acceso de un cliente. */
@Getter
@Setter
public class LoginEstatusRequest {

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un número positivo")
    private Integer idCliente;

    @NotNull(message = "El campo 'activo' es obligatorio (true o false)")
    private Boolean activo;
}
