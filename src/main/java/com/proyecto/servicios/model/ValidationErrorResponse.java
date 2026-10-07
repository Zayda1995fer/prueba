package com.proyecto.servicios.model;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

/**
 * Respuesta para cuando falla la validación de un @RequestBody
 * (@Valid). A diferencia de GenericResponse (un solo "mensaje" de
 * texto), aquí cada campo que falló viene como un elemento aparte en
 * "errores", para que se pueda mostrar cada error junto a su campo en
 * el formulario, en vez de un solo bloque de texto con todo junto.
 *
 * Ejemplo de respuesta:
 * {
 *   "codigo": 400,
 *   "mensaje": "Hay 2 campos con errores de validación",
 *   "errores": [
 *     { "campo": "nacionalidadId", "mensaje": "La nacionalidad es obligatoria" },
 *     { "campo": "telefonoMovil", "mensaje": "El teléfono móvil debe contener exactamente 10 dígitos" }
 *   ]
 * }
 */
@Getter
@Setter
@NoArgsConstructor
public class ValidationErrorResponse {
    private Integer codigo;
    private String mensaje;
    private List<ErrorCampo> errores;
}