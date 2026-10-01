package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class RfcDuplicadoException extends NegocioException {
    public RfcDuplicadoException(String rfc) {
        super("Ya existe un cliente registrado con el RFC " + rfc, HttpStatus.CONFLICT);
    }
}
