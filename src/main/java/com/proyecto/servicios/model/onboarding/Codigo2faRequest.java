package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Codigo2faRequest {

    @NotNull(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El número de cuenta debe tener exactamente 10 dígitos")
    private String numeroCuenta;

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un número positivo")
    private Integer idUser;
}
