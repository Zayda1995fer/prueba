package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public class CurpDuplicadaException extends NegocioException {

    public CurpDuplicadaException(String curp) {
        super("Ya existe un cliente registrado con la CURP " + curp, HttpStatus.CONFLICT, "curp");
    }
}