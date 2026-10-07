package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

// Para reglas de negocio que no se pueden expresar con las anotaciones
// de Bean Validation (por ejemplo, "el cliente debe ser mayor de edad",
// que depende de la fecha actual, no solo del formato del dato).
public class ValidacionException extends NegocioException {

    public ValidacionException(String mensaje) {
        super(mensaje, HttpStatus.BAD_REQUEST);
    }

    public ValidacionException(String mensaje, String campo) {
        super(mensaje, HttpStatus.BAD_REQUEST, campo);
    }
}