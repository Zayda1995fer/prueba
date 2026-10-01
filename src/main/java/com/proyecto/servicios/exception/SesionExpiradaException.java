package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class SesionExpiradaException extends NegocioException {
    public SesionExpiradaException(String mensaje) {
        super(mensaje, HttpStatus.UNAUTHORIZED);
    }
}