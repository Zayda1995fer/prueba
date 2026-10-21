package com.proyecto.servicios.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.http.HttpStatus;

@Getter
@Setter
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class RespuestaExitosa<T> {

    private boolean success = true;
    private Integer codigo;
    private String message;
    private T data;

    public static <T> RespuestaExitosa<T> de(HttpStatus estatus, String mensaje, T datos) {
        RespuestaExitosa<T> respuesta = new RespuestaExitosa<>();
        respuesta.setCodigo(estatus.value());
        respuesta.setMessage(mensaje);
        respuesta.setData(datos);
        return respuesta;
    }
}