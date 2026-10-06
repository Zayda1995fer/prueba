package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Datos para darle a un cliente ya registrado su acceso (usuario y
 * contraseña) al sistema. El dato biométrico es opcional porque no
 * todos los canales de captura cuentan con un lector biométrico.
 */
@Getter
@Setter
@NoArgsConstructor
public class LoginRegistroRequest {

    @NotBlank(message = "El usuario es obligatorio")
    @Size(min = 4, max = 50, message = "El usuario debe tener entre 4 y 50 caracteres")
    @Pattern(regexp = "^[A-Za-z0-9._-]+$",
            message = "El usuario solo puede contener letras, números y . _ -, sin espacios ni acentos")
    private String usuario;

    // No se restringe el juego de caracteres (una contraseña fuerte
    // justamente necesita símbolos); solo se exige longitud mínima y que
    // combine letras y números, para que no sea solo "11111111".
    @NotBlank(message = "La contraseña es obligatoria")
    @Size(min = 8, message = "La contraseña debe tener al menos 8 caracteres")
    @Pattern(regexp = "^(?=.*[A-Za-zÁÉÍÓÚÑáéíóúñ])(?=.*\\d).+$",
            message = "La contraseña debe combinar al menos una letra y un número")
    private String contrasena;

    // Opcional: valor numérico simplificado que representaría la plantilla
    // biométrica capturada (ver Login.java para la justificación completa)
    @PositiveOrZero(message = "El dato biométrico no puede ser un valor negativo")
    private Double datoBiometrico;
}