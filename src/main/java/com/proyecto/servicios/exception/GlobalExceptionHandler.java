package com.proyecto.servicios.exception;

import com.proyecto.servicios.model.GenericResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<GenericResponse> manejarNegocioException(NegocioException ex) {
        log.error("Error de negocio: {} -> HTTP {}", ex.getMessage(), ex.getHttpStatus().value());
        return construirRespuesta(ex.getHttpStatus(), ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<GenericResponse> manejarValidacion(MethodArgumentNotValidException ex) {
        String mensaje = ex.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getField() + ": " + error.getDefaultMessage())
                .collect(Collectors.joining(" | "));

        log.error("Error de validación de datos de entrada: {}", mensaje);
        return construirRespuesta(HttpStatus.BAD_REQUEST, mensaje);
    }

    private ResponseEntity<GenericResponse> construirRespuesta(HttpStatus status, String mensaje) {
        GenericResponse respuesta = new GenericResponse();
        respuesta.setCodigo(status.value());
        respuesta.setMensaje(mensaje);
        return ResponseEntity.status(status).body(respuesta);
    }
}
