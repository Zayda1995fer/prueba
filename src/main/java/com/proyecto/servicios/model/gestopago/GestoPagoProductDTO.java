package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

// Representa un solo producto dentro de la lista que devuelve el servicio externo.
// OJO: estos nombres de campo son un ejemplo. Hay que revisarlos y ajustarlos
// cuando se vea la respuesta real del servicio.
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoProductDTO {

    private String codigo;
    private String nombre;
    private String descripcion;
    private Double precio;
    private Boolean activo;
}
