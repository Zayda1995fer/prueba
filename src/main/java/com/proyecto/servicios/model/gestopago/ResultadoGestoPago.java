package com.proyecto.servicios.model.gestopago;

import com.proyecto.servicios.exception.CatalogoError;
import lombok.Getter;

/**
 * Lo que sabemos de una operación después de hablar con GestoPago.
 * La diferencia entre RECHAZADO e INDETERMINADO es la base de todo el flujo:
 *  - RECHAZADO: el proveedor confirmó que NO aplicó el pago → se devuelve el saldo.
 *  - INDETERMINADO: no sabemos si lo aplicó (timeout, error 5xx...) → el saldo se queda
 *    retenido hasta conciliar; jamás se asume que falló.
 */
@Getter
public final class ResultadoGestoPago {

    public enum Tipo { EXITOSO, RECHAZADO, INDETERMINADO }

    private final Tipo tipo;
    private final String codigo;
    private final String folio;
    private final CatalogoError error;

    private ResultadoGestoPago(Tipo tipo, String codigo, String folio, CatalogoError error) {
        this.tipo = tipo;
        this.codigo = codigo;
        this.folio = folio;
        this.error = error;
    }

    public static ResultadoGestoPago exitoso(String codigo, String folio) {
        return new ResultadoGestoPago(Tipo.EXITOSO, codigo, folio, null);
    }

    public static ResultadoGestoPago rechazado(String codigo, CatalogoError error) {
        return new ResultadoGestoPago(Tipo.RECHAZADO, codigo, null, error);
    }

    public static ResultadoGestoPago indeterminado(String codigo) {
        return new ResultadoGestoPago(Tipo.INDETERMINADO, codigo, null, null);
    }
}
