package com.proyecto.servicios.model.onboarding;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Resultado de un pago. El destino (teléfono o referencia) se devuelve enmascarado. */
@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class PagoResponse {

    private String idSolicitud;
    /** EXITOSO, POR_CONCILIAR, RECHAZADO o REVISION_MANUAL */
    private String estatus;
    private String folioTransaccion;
    private Integer idServicio;
    private Integer idProducto;
    private String producto;
    private BigDecimal monto;
    private String tipoDestino;
    private String destino;
    private BigDecimal saldoActual;
    private LocalDateTime fechaPago;
}
