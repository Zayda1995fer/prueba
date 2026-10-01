package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

// Esta clase representa la respuesta completa que nos manda el servicio externo.
// "data" es la lista de productos que realmente nos interesa.
@Data
@JsonIgnoreProperties(ignoreUnknown = true) // Si el servicio manda campos que no usamos, se ignoran sin romper nada
public class GestoPagoProductListResponse {

    private Integer status;
    private String message;
    private List<GestoPagoProductDTO> data;
}
