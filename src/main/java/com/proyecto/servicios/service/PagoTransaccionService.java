package com.proyecto.servicios.service;

import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago;
import com.proyecto.servicios.model.onboarding.PagoRequest;

/**
 * Los dos pasos de base de datos del pago, cada uno en su propia transacción corta.
 * La llamada a GestoPago queda FUERA de ambos: así no se mantiene un bloqueo de la
 * cuenta mientras se espera una respuesta de red.
 */
public interface PagoTransaccionService {

    /** Resultado del primer paso: el pago registrado y si en realidad ya existía (solicitud repetida). */
    record Reserva(Pago pago, boolean repeticion) {
    }

    /**
     * Paso 1: valida al cliente, la cuenta, el saldo y el 2FA; descuenta el saldo y registra el pago
     * como PENDIENTE. Si la solicitud ya existe devuelve el pago original sin cobrar otra vez.
     */
    Reserva reservar(PagoRequest request, ProductoPago producto);

    /**
     * Paso 2: aplica el resultado del proveedor. EXITOSO confirma, RECHAZADO devuelve el saldo (una
     * sola vez) e INDETERMINADO deja el saldo retenido para conciliar. Si el pago ya tenía
     * resultado final no hace nada (es seguro llamarlo dos veces).
     */
    Pago registrarResultado(Integer pagoId, ResultadoGestoPago resultado, boolean desdeConciliacion);
}
