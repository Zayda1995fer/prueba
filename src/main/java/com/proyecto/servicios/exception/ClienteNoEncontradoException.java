package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class ClienteNoEncontradoException extends NegocioException {
    public ClienteNoEncontradoException(String detalle) {
        super("No se encontró el cliente: " + detalle, HttpStatus.NOT_FOUND);
    }
}