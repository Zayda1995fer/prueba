package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

// Demasiados intentos fallidos de inicio de sesión: bloqueo temporal (HTTP 429).
public class DemasiadosIntentosException extends NegocioException {

    public DemasiadosIntentosException(String mensaje) {
        super(mensaje, HttpStatus.TOO_MANY_REQUESTS);
    }
}