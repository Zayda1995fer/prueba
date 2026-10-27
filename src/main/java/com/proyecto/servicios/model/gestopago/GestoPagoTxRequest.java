package com.proyecto.servicios.model.gestopago;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** Cuerpo del sendTx. Los nombres de los campos son los del diagrama del profesor. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class GestoPagoTxRequest {

    private Integer idProd;
    private Integer idUser;
    private String telefonoReferencia;
    private String horaLocal;
    private String monto;
    private String upc;
    /** Referencia propia de la operación: con ella se consulta el resultado al conciliar. */
    private String idTransaccion;
}
