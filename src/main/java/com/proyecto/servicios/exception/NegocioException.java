package com.proyecto.servicios.exception;


import org.springframework.http.HttpStatus;

public abstract class NegocioException extends RuntimeException {

    private final HttpStatus httpStatus;

    protected NegocioException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
