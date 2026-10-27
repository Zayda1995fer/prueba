package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago;

/**
 * Única puerta hacia GestoPago. Traduce todo lo que puede pasar en la red
 * (éxito, rechazo, timeout, errores 4xx/5xx) a un ResultadoGestoPago, para que el
 * resto del sistema decida con una regla simple y no con excepciones de HTTP.
 */
public interface GestoPagoGateway {

    /** Envía el pago al proveedor. Nunca lanza excepción ni reintenta el envío. */
    ResultadoGestoPago enviarPago(Pago pago, ProductoPago producto);

    /** Pregunta al proveedor qué pasó con un pago ya enviado (conciliación). Nunca lanza excepción. */
    ResultadoGestoPago consultarPago(Pago pago);
}
