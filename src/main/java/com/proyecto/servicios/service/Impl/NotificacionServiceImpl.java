package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.service.NotificacionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Envía el correo desde la cuenta configurada en spring.mail.*.
 * Si el envío falla solo se registra en el log: el pago ya se hizo y no se revierte.
 */
@Slf4j
@Service
public class NotificacionServiceImpl implements NotificacionService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    private final JavaMailSender mailSender;

    @Value("${app.notificaciones.activo:false}")
    private boolean activo;

    @Value("${app.notificaciones.remitente:}")
    private String remitente;

    public NotificacionServiceImpl(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    @Override
    public void notificarPago(String correo, String nombreCliente, String numeroCuenta, String producto,
                              BigDecimal monto, BigDecimal saldoActual, String folio, LocalDateTime fecha) {
        if (!activo) {
            log.info("Notificaciones por correo desactivadas, no se envía aviso");
            return;
        }
        try {
            SimpleMailMessage mensaje = new SimpleMailMessage();
            mensaje.setFrom(remitente);
            mensaje.setTo(correo);
            mensaje.setSubject("Aviso de movimiento: pago de servicio realizado");
            mensaje.setText(String.format(Locale.US,
                    "Hola %s,%n%n"
                            + "Se realizó un cargo en tu cuenta terminación %s.%n%n"
                            + "Concepto: %s%n"
                            + "Monto: $%,.2f%n"
                            + "Saldo disponible: $%,.2f%n"
                            + "Folio: %s%n"
                            + "Fecha: %s%n%n"
                            + "Si no reconoces este movimiento, comunícate con nosotros.",
                    nombreCliente, terminacion(numeroCuenta), producto, monto, saldoActual,
                    folio == null ? "en confirmación" : folio, fecha.format(FORMATO_FECHA)));
            mailSender.send(mensaje);
            log.info("Aviso de pago enviado");
        } catch (Exception e) {
            // Solo el tipo de error: el mensaje de la excepción podría traer datos del correo
            log.error("No fue posible enviar el aviso de pago ({})", e.getClass().getSimpleName());
        }
    }

    private String terminacion(String numeroCuenta) {
        return numeroCuenta.length() <= 4 ? numeroCuenta : numeroCuenta.substring(numeroCuenta.length() - 4);
    }
}
