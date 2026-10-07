package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

// Se usa cuando el cliente ya existe por más de un motivo a la vez,
// o para casos generales de "esto ya está registrado" distintos a
// CURP/RFC (por ejemplo, correo o usuario duplicado). HTTP 409.
public class ClienteYaRegistradoException extends NegocioException {

    public ClienteYaRegistradoException(String mensaje) {
        super(mensaje, HttpStatus.CONFLICT);
    }

    public ClienteYaRegistradoException(String mensaje, String campo) {
        super(mensaje, HttpStatus.CONFLICT, campo);
    }
}