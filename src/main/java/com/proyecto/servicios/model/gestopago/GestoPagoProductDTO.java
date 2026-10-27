package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoProductDTO {

    private String codigo;
    private String nombre;
    private String descripcion;
    private Double precio;
    private Boolean activo;
}
