package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;


@Getter
@Setter
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(max = 50, message = "El usuario no debe exceder 50 caracteres")
    private String usuario;

    @JsonDeserialize(using = StringDeserializer.class)
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(max = 72, message = "La contraseña no debe exceder 72 caracteres")
    private String contrasena;
}