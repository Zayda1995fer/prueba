package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Respuesta de GestoPago (los campos que no usamos se ignoran). */
@Getter
@Setter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GestoPagoTxResponse {

    private String codigo;
    private String mensaje;
    private String folioTransaccion;
}
