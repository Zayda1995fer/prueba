package com.proyecto.servicios.service;

/**
 * Resuelve los pagos de los que no se conoce el resultado final (timeout, corte de red,
 * caída de la aplicación a medio pago). Consulta al proveedor; nunca reenvía el pago.
 */
public interface ConciliacionService {

    /** Procesa un lote de pagos pendientes y devuelve cuántos se revisaron. */
    int conciliarPendientes();
}
