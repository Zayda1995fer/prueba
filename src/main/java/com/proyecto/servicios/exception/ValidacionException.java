package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ValidacionException extends NegocioException {
    public ValidacionException(String mensaje) {
        super(mensaje, HttpStatus.BAD_REQUEST);
    }
}