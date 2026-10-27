package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago;
import com.proyecto.servicios.repositorys.onboarding.PagoRepository;
import com.proyecto.servicios.service.GestoPagoGateway;
import com.proyecto.servicios.service.PagoTransaccionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ConciliacionServiceImplTest {

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private GestoPagoGateway gateway;
    @Mock
    private PagoTransaccionService transacciones;

    private ConciliacionServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new ConciliacionServiceImpl(pagoRepository, gateway, transacciones);
        ReflectionTestUtils.setField(servicio, "activa", true);
        ReflectionTestUtils.setField(servicio, "esperaSegundos", 120);
        ReflectionTestUtils.setField(servicio, "maxIntentos", 10);
        ReflectionTestUtils.setField(servicio, "tamanoLote", 20);
    }

    private Pago pago(int id) {
        Pago p = new Pago();
        p.setId(id);
        p.setEstatus(Pago.ESTATUS_POR_CONCILIAR);
        return p;
    }

    private void hayPendientes(Pago... pagos) {
        when(pagoRepository.buscarPorConciliar(anyCollection(), any(LocalDateTime.class), anyInt(), any(Pageable.class)))
                .thenReturn(List.of(pagos));
    }

    @Test
    void consultaCadaPagoPendienteYRegistraSuResultadoComoConciliacion() {
        Pago a = pago(1);
        Pago b = pago(2);
        hayPendientes(a, b);
        ResultadoGestoPago exito = ResultadoGestoPago.exitoso("0", "F1");
        ResultadoGestoPago noAplicado = ResultadoGestoPago.rechazado("10", CatalogoError.PAGO_RECHAZADO);
        when(gateway.consultarPago(a)).thenReturn(exito);
        when(gateway.consultarPago(b)).thenReturn(noAplicado);

        assertThat(servicio.conciliarPendientes()).isEqualTo(2);

        verify(transacciones).registrarResultado(1, exito, true);
        verify(transacciones).registrarResultado(2, noAplicado, true);
    }

    @Test
    void nuncaReenviaElPago() {
        hayPendientes(pago(1));
        when(gateway.consultarPago(any())).thenReturn(ResultadoGestoPago.indeterminado("HTTP_500"));

        servicio.conciliarPendientes();

        verify(gateway, never()).enviarPago(any(), any());
    }

    @Test
    void unPagoConProblemasNoDetieneLosDemas() {
        Pago a = pago(1);
        Pago b = pago(2);
        hayPendientes(a, b);
        when(gateway.consultarPago(a)).thenThrow(new IllegalStateException("falló"));
        ResultadoGestoPago exito = ResultadoGestoPago.exitoso("0", "F2");
        when(gateway.consultarPago(b)).thenReturn(exito);

        servicio.conciliarPendientes();

        verify(transacciones).registrarResultado(eq(2), eq(exito), eq(true));
        verify(transacciones, never()).registrarResultado(eq(1), any(), anyBoolean());
    }

    @Test
    void sinPendientes_noHaceNada() {
        hayPendientes();
        assertThat(servicio.conciliarPendientes()).isZero();
        verifyNoInteractions(gateway, transacciones);
    }

    @Test
    void tareaProgramada_apagadaNoConsultaNada() {
        ReflectionTestUtils.setField(servicio, "activa", false);
        servicio.tareaProgramada();
        verifyNoInteractions(pagoRepository, gateway, transacciones);
    }

    @Test
    void tareaProgramada_encendidaConcilia() {
        hayPendientes();
        servicio.tareaProgramada();
        verify(pagoRepository).buscarPorConciliar(anyCollection(), any(LocalDateTime.class), eq(10), any(Pageable.class));
    }
}
