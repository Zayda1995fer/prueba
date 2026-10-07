package com.proyecto.servicios.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Error de UN campo del request: nombre del campo y qué está mal en él. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ErrorCampo {
    private String field;
    private String message;
}