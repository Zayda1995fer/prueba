package com.proyecto.servicios.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Avisos por correo al cliente (estilo notificación de movimiento de banco). */
public interface NotificacionService {

    /** Avisa al cliente que se hizo un cargo por pago de servicio. Nunca lanza excepción. */
    void notificarPago(String correo, String nombreCliente, String numeroCuenta, String producto,
                       BigDecimal monto, BigDecimal saldoActual, String folio, LocalDateTime fecha);
}
