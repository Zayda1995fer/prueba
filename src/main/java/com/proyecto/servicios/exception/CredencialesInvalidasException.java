package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CredencialesInvalidasException extends NegocioException {
    public CredencialesInvalidasException(String mensaje) {
        super(mensaje, HttpStatus.UNAUTHORIZED);
    }
}