package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.deser.std.StringDeserializer;
import com.proyecto.servicios.validation.ContrasenaSegura;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.proyecto.servicios.validation.ReglasValidacion.USUARIO;


@Getter
@Setter
@NoArgsConstructor
public class LoginRegistroRequest {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 4, max = 50, message = "El usuario debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = USUARIO,
            message = "El usuario solo puede contener letras, números y . _ -, sin espacios ni acentos")
    private String usuario;

    // @JsonDeserialize: la contraseña se lee tal cual (sin trim); si trae
    // espacios, ContrasenaSegura la rechaza en lugar de modificarla en silencio.
    @JsonDeserialize(using = StringDeserializer.class)
    @NotBlank(message = "La contraseña es obligatoria")
    @ContrasenaSegura
    private String contrasena;

    // Opcional: valor numérico simplificado que representaría la plantilla
    // biométrica capturada (ver Login.java para la justificación completa)
    @PositiveOrZero(message = "El dato biométrico no puede ser un valor negativo")
    @DecimalMax(value = "1000000", message = "El dato biométrico excede el valor máximo permitido")
    private Double datoBiometrico;
}