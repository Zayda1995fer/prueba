package com.proyecto.servicios.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class NotificacionServiceImplTest {

    @Mock
    private JavaMailSender mailSender;

    private NotificacionServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new NotificacionServiceImpl(mailSender);
        ReflectionTestUtils.setField(servicio, "activo", true);
        ReflectionTestUtils.setField(servicio, "remitente", "banco@gmail.com");
    }

    private void enviar() {
        servicio.notificarPago("cliente@correo.com", "Ana", "1234567890", "Pago de luz (CFE)",
                new BigDecimal("350.00"), new BigDecimal("1650.00"), "FOL123", LocalDateTime.now());
    }

    @Test
    void envia_correoConMontoFolioYSoloLaTerminacionDeLaCuenta() {
        enviar();

        ArgumentCaptor<SimpleMailMessage> correo = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(correo.capture());
        assertThat(correo.getValue().getTo()).containsExactly("cliente@correo.com");
        assertThat(correo.getValue().getFrom()).isEqualTo("banco@gmail.com");
        assertThat(correo.getValue().getText())
                .contains("350.00", "1,650.00", "FOL123", "7890")
                .doesNotContain("1234567890");
    }

    @Test
    void desactivado_noEnviaNada() {
        ReflectionTestUtils.setField(servicio, "activo", false);

        enviar();

        verify(mailSender, never()).send(any(SimpleMailMessage.class));
    }

    @Test
    void siFallaElEnvio_noLanzaExcepcion() {
        doThrow(new MailSendException("smtp caído")).when(mailSender).send(any(SimpleMailMessage.class));

        assertThatCode(this::enviar).doesNotThrowAnyException();
    }
}
