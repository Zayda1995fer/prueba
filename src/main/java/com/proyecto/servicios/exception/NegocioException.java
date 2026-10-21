package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/**
 * Única excepción de negocio de la aplicación. En lugar de una clase por
 * cada error, se indica qué pasó con una entrada de CatalogoError (que
 * trae el código HTTP, el campo y el texto del mensaje) y, si el mensaje
 * lo necesita, los valores que rellenan sus marcadores %s:
 *
 *     throw new NegocioException(CatalogoError.CURP_DUPLICADA, curp);
 *
 * GlobalExceptionHandler la atrapa una sola vez y arma la respuesta.
 */
public class NegocioException extends RuntimeException {

    private final CatalogoError error;

    public NegocioException(CatalogoError error, Object... argumentos) {
        super(error.mensaje(argumentos));
        this.error = error;
    }

    public CatalogoError getError() {
        return error;
    }

    public HttpStatus getHttpStatus() {
        return error.getHttpStatus();
    }

    /** Nombre del campo del request que originó el error; null si no aplica a un campo. */
    public String getCampo() {
        return error.getCampo();
    }
}
