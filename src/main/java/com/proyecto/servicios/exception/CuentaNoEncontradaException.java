package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CuentaNoEncontradaException extends NegocioException {
    public CuentaNoEncontradaException(String numeroCuenta) {
        super("No se encontró la cuenta con número: " + numeroCuenta, HttpStatus.NOT_FOUND);
    }
}