package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

public abstract class NegocioException extends RuntimeException {

    private final HttpStatus httpStatus;
    private final String campo;

    protected NegocioException(String message, HttpStatus httpStatus) {
        this(message, httpStatus, null);
    }

    protected NegocioException(String message, HttpStatus httpStatus, String campo) {
        super(message);
        this.httpStatus = httpStatus;
        this.campo = campo;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /** Nombre del campo del request que originó el error; null si no aplica a un campo. */
    public String getCampo() {
        return campo;
    }
}