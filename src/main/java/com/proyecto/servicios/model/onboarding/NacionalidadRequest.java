package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.proyecto.servicios.validation.ReglasValidacion.NOMBRE_PERSONA;

/**
 * Objeto que se captura dentro del arreglo "nacionalidad" del cliente:
 * { "id": 1, "nombre": "Mexicana" }.
 *
 * Aquí solo se valida la forma. Que el id exista y que el nombre
 * corresponda a ese id (aceptando masculino/femenino, mayúsculas y
 * acentos: Mexicana / Mexicano / MEXICANA) se valida en ClienteServiceImpl.
 */
@Getter
@Setter
@NoArgsConstructor
public class NacionalidadRequest {

    @NotNull(message = "El id de la nacionalidad es obligatorio")
    @Positive(message = "El id de la nacionalidad debe ser un entero positivo del catálogo")
    private Integer id;

    @NotBlank(message = "El nombre de la nacionalidad es obligatorio")
    @Size(min = 2, max = 40, message = "El nombre de la nacionalidad debe tener entre 2 y 40 caracteres")
    @Pattern(regexp = NOMBRE_PERSONA,
            message = "El nombre de la nacionalidad solo debe contener letras (con acentos o ñ) separadas por un espacio")
    private String nombre;
}