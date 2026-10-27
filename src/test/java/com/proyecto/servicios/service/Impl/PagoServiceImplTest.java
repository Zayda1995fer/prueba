package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago;
import com.proyecto.servicios.model.onboarding.PagoConsultaRequest;
import com.proyecto.servicios.model.onboarding.PagoRequest;
import com.proyecto.servicios.model.onboarding.PagoResponse;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.PagoRepository;
import com.proyecto.servicios.repositorys.onboarding.ProductoPagoRepository;
import com.proyecto.servicios.service.GestoPagoGateway;
import com.proyecto.servicios.service.PagoTransaccionService;
import com.proyecto.servicios.service.PagoTransaccionService.Reserva;
import com.proyecto.servicios.validation.ValidadorPago;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoServiceImplTest {

    private static final String ID_SOLICITUD = "3f2b8c1e-9a4d-4c7e-8b21-5d6e7f8a9b0c";

    @Mock
    private ValidadorPago validador;
    @Mock
    private PagoTransaccionService transacciones;
    @Mock
    private GestoPagoGateway gateway;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private ProductoPagoRepository productoRepository;
    @Mock
    private CuentaRepository cuentaRepository;

    private PagoServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new PagoServiceImpl(validador, transacciones, gateway, pagoRepository, productoRepository,
                cuentaRepository);
    }

    // ---------------- datos de prueba ----------------

    private PagoRequest request() {
        PagoRequest r = new PagoRequest();
        r.setIdSolicitud(ID_SOLICITUD);
        r.setNumeroCuenta("1234567890");
        r.setIdUser(5);
        r.setIdServicio(10);
        r.setIdProducto(1);
        r.setMonto(new BigDecimal("350.00"));
        r.setReferencia("ABC12345");
        r.setCodigo2fa("123456");
        return r;
    }

    private ProductoPago producto() {
        ProductoPago p = new ProductoPago();
        p.setIdProducto(1);
        p.setIdServicio(10);
        p.setNombre("Pago de luz (CFE)");
        p.setTipoDestino(ProductoPago.DESTINO_REFERENCIA);
        return p;
    }

    private Pago pago(String estatus) {
        Pago p = new Pago();
        p.setId(7);
        p.setIdSolicitud(ID_SOLICITUD);
        p.setCuentaId(9);
        p.setClienteId(5);
        p.setIdProducto(1);
        p.setIdServicio(10);
        p.setMonto(new BigDecimal("350.00"));
        p.setDestino("ABC12345");
        p.setEstatus(estatus);
        p.setFechaCreacion(LocalDateTime.of(2026, 10, 27, 12, 0));
        return p;
    }

    private void cuentaConSaldo(String saldo) {
        Cuenta c = new Cuenta();
        c.setId(9);
        c.setSaldo(new BigDecimal(saldo));
        when(cuentaRepository.findById(9)).thenReturn(Optional.of(c));
    }

    private void validacionOk() {
        when(validador.validar(any(PagoRequest.class))).thenReturn(producto());
    }

    private void assertError(Runnable accion, CatalogoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(NegocioException.class,
                ex -> assertThat(ex.getError()).isEqualTo(esperado));
    }

    // ---------------- pagar ----------------

    @Test
    void pagar_exitoso_siguelosPasosEnOrdenYRegresaElFolio() {
        validacionOk();
        Pago pendiente = pago(Pago.ESTATUS_PENDIENTE);
        when(transacciones.reservar(any(), any())).thenReturn(new Reserva(pendiente, false));
        ResultadoGestoPago exito = ResultadoGestoPago.exitoso("0", "FOL-1");
        when(gateway.enviarPago(eq(pendiente), any())).thenReturn(exito);
        Pago confirmado = pago(Pago.ESTATUS_EXITOSO);
        confirmado.setFolioTransaccion("FOL-1");
        when(transacciones.registrarResultado(7, exito, false)).thenReturn(confirmado);
        cuentaConSaldo("1650.00");

        PagoResponse response = servicio.pagar(request());

        assertThat(response.getEstatus()).isEqualTo("EXITOSO");
        assertThat(response.getFolioTransaccion()).isEqualTo("FOL-1");
        assertThat(response.getSaldoActual()).isEqualByComparingTo("1650.00");
        assertThat(response.getDestino()).isEqualTo("****2345");   // enmascarado
        assertThat(response.getTipoDestino()).isEqualTo(ProductoPago.DESTINO_REFERENCIA);

        InOrder orden = inOrder(validador, transacciones, gateway);
        orden.verify(validador).validar(any(PagoRequest.class));
        orden.verify(transacciones).reservar(any(), any());
        orden.verify(gateway).enviarPago(any(), any());
        orden.verify(transacciones).registrarResultado(eq(7), any(), eq(false));
    }

    @Test
    void pagar_sinRespuestaDelProveedor_regresaPorConciliarSinFallar() {
        validacionOk();
        Pago pendiente = pago(Pago.ESTATUS_PENDIENTE);
        when(transacciones.reservar(any(), any())).thenReturn(new Reserva(pendiente, false));
        ResultadoGestoPago duda = ResultadoGestoPago.indeterminado("SIN_RESPUESTA");
        when(gateway.enviarPago(any(), any())).thenReturn(duda);
        when(transacciones.registrarResultado(7, duda, false)).thenReturn(pago(Pago.ESTATUS_POR_CONCILIAR));
        cuentaConSaldo("1650.00");

        PagoResponse response = servicio.pagar(request());

        assertThat(response.getEstatus()).isEqualTo("POR_CONCILIAR");
        assertThat(response.getFolioTransaccion()).isNull();
        assertThat(response.getSaldoActual()).isEqualByComparingTo("1650.00");   // el saldo sigue retenido
    }

    @Test
    void pagar_rechazadoPorElProveedor_lanzaPagoRechazadoConElCodigo() {
        validacionOk();
        when(transacciones.reservar(any(), any())).thenReturn(new Reserva(pago(Pago.ESTATUS_PENDIENTE), false));
        ResultadoGestoPago rechazo = ResultadoGestoPago.rechazado("5", CatalogoError.PAGO_RECHAZADO);
        when(gateway.enviarPago(any(), any())).thenReturn(rechazo);
        Pago rechazado = pago(Pago.ESTATUS_RECHAZADO);
        rechazado.setCodigoRespuesta("5");
        when(transacciones.registrarResultado(7, rechazo, false)).thenReturn(rechazado);

        assertThatThrownBy(() -> servicio.pagar(request()))
                .isInstanceOfSatisfying(NegocioException.class, ex -> {
                    assertThat(ex.getError()).isEqualTo(CatalogoError.PAGO_RECHAZADO);
                    assertThat(ex.getMessage()).contains("código 5");
                });
    }

    @Test
    void pagar_sinTokenDelProveedor_lanzaElErrorDeServicioNoDisponible() {
        validacionOk();
        when(transacciones.reservar(any(), any())).thenReturn(new Reserva(pago(Pago.ESTATUS_PENDIENTE), false));
        ResultadoGestoPago sinToken = ResultadoGestoPago.rechazado("SIN_TOKEN", CatalogoError.GESTOPAGO_SIN_TOKEN);
        when(gateway.enviarPago(any(), any())).thenReturn(sinToken);
        when(transacciones.registrarResultado(7, sinToken, false)).thenReturn(pago(Pago.ESTATUS_RECHAZADO));

        assertError(() -> servicio.pagar(request()), CatalogoError.GESTOPAGO_SIN_TOKEN);
    }

    @Test
    void pagar_solicitudRepetida_noVuelveACobrarNiLlamaAlProveedor() {
        validacionOk();
        Pago confirmado = pago(Pago.ESTATUS_EXITOSO);
        confirmado.setFolioTransaccion("FOL-1");
        when(transacciones.reservar(any(), any())).thenReturn(new Reserva(confirmado, true));
        cuentaConSaldo("1650.00");

        PagoResponse response = servicio.pagar(request());

        assertThat(response.getEstatus()).isEqualTo("EXITOSO");
        verifyNoInteractions(gateway);
        verify(transacciones, never()).registrarResultado(any(), any(), anyBoolean());
    }

    @Test
    void pagar_solicitudRepetidaDeUnPagoRechazado_repiteElMismoError() {
        validacionOk();
        Pago rechazado = pago(Pago.ESTATUS_RECHAZADO);
        rechazado.setCodigoRespuesta("5");
        when(transacciones.reservar(any(), any())).thenReturn(new Reserva(rechazado, true));

        assertError(() -> servicio.pagar(request()), CatalogoError.PAGO_RECHAZADO);
        verifyNoInteractions(gateway);
    }

    @Test
    void pagar_solicitudRepetidaMientrasSigueEnCurso_sePresentaComoPorConciliar() {
        validacionOk();
        when(transacciones.reservar(any(), any())).thenReturn(new Reserva(pago(Pago.ESTATUS_PENDIENTE), true));
        cuentaConSaldo("1650.00");

        assertThat(servicio.pagar(request()).getEstatus()).isEqualTo("POR_CONCILIAR");
    }

    @Test
    void pagar_validacionDeCatalogoFalla_noReservaNiLlamaAlProveedor() {
        when(validador.validar(any(PagoRequest.class)))
                .thenThrow(new NegocioException(CatalogoError.MONTO_FUERA_DE_RANGO, "10", "100"));

        assertError(() -> servicio.pagar(request()), CatalogoError.MONTO_FUERA_DE_RANGO);
        verifyNoInteractions(transacciones, gateway);
    }

    @Test
    void pagar_reservaFalla_noLlamaAlProveedor() {
        validacionOk();
        when(transacciones.reservar(any(), any())).thenThrow(new NegocioException(CatalogoError.SALDO_INSUFICIENTE));

        assertError(() -> servicio.pagar(request()), CatalogoError.SALDO_INSUFICIENTE);
        verifyNoInteractions(gateway);
    }

    // ---------------- consultar ----------------

    @Test
    void consultar_regresaElEstadoDelPagoDelCliente() {
        Pago confirmado = pago(Pago.ESTATUS_EXITOSO);
        confirmado.setFolioTransaccion("FOL-1");
        when(pagoRepository.findByIdSolicitud(ID_SOLICITUD)).thenReturn(Optional.of(confirmado));
        when(productoRepository.findById(1)).thenReturn(Optional.of(producto()));
        cuentaConSaldo("1650.00");

        PagoConsultaRequest request = new PagoConsultaRequest();
        request.setIdSolicitud(ID_SOLICITUD.toUpperCase());
        request.setIdUser(5);

        PagoResponse response = servicio.consultarPago(request);

        assertThat(response.getEstatus()).isEqualTo("EXITOSO");
        assertThat(response.getFolioTransaccion()).isEqualTo("FOL-1");
    }

    @Test
    void consultar_pagoDeOtroCliente_seTrataComoInexistente() {
        when(pagoRepository.findByIdSolicitud(ID_SOLICITUD)).thenReturn(Optional.of(pago(Pago.ESTATUS_EXITOSO)));
        PagoConsultaRequest request = new PagoConsultaRequest();
        request.setIdSolicitud(ID_SOLICITUD);
        request.setIdUser(99);

        assertError(() -> servicio.consultarPago(request), CatalogoError.PAGO_NO_ENCONTRADO);
    }

    @Test
    void consultar_solicitudInexistente() {
        when(pagoRepository.findByIdSolicitud(ID_SOLICITUD)).thenReturn(Optional.empty());
        PagoConsultaRequest request = new PagoConsultaRequest();
        request.setIdSolicitud(ID_SOLICITUD);
        request.setIdUser(5);

        assertError(() -> servicio.consultarPago(request), CatalogoError.PAGO_NO_ENCONTRADO);
    }
}
