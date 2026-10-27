package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;


@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoProductListResponse {

    private Integer status;
    private String message;
    private List<GestoPagoProductDTO> data;
}
