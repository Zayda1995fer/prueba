package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.model.gestopago.GestoPagoConsultaRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago.Tipo;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.Request;
import feign.Response;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class GestoPagoGatewayImplTest {

    @Mock
    private GestoPagoTxClient client;
    @Mock
    private GestoPagoTokenService tokenService;

    private GestoPagoGatewayImpl gateway;

    @BeforeEach
    void setUp() {
        gateway = new GestoPagoGatewayImpl(client, tokenService);
        ReflectionTestUtils.setField(gateway, "url", "https://gestopago.portalventas.net");
        ReflectionTestUtils.setField(gateway, "idDistribuidor", 83);
        ReflectionTestUtils.setField(gateway, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(gateway, "idUser", 83);
        ReflectionTestUtils.setField(gateway, "upc", "");
        ReflectionTestUtils.setField(gateway, "zonaHoraria", "America/Mexico_City");
        ReflectionTestUtils.setField(gateway, "codigoExito", "0");
        ReflectionTestUtils.setField(gateway, "codigosPendientes", "99");
        ReflectionTestUtils.setField(gateway, "codigosNoAplicada", "10,11");
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token("TOKEN-SECRETO")));
    }

    // ---------------- datos de prueba ----------------

    private GestoPagoToken token(String valor) {
        GestoPagoToken t = new GestoPagoToken();
        t.setToken(valor);
        return t;
    }

    private Pago pago() {
        Pago p = new Pago();
        p.setId(7);
        p.setReferenciaProveedor("ref0001");
        p.setDestino("ABC12345");
        p.setMonto(new BigDecimal("350.00"));
        return p;
    }

    private ProductoPago producto() {
        ProductoPago p = new ProductoPago();
        p.setIdProducto(1);
        p.setIdServicio(10);
        return p;
    }

    private GestoPagoTxResponse respuesta(String codigo, String folio) {
        GestoPagoTxResponse r = new GestoPagoTxResponse();
        r.setCodigo(codigo);
        r.setFolioTransaccion(folio);
        return r;
    }

    private static Request peticion() {
        return Request.create(Request.HttpMethod.POST, "http://localhost/x", Map.of(), null,
                StandardCharsets.UTF_8, null);
    }

    private static FeignException http(int status) {
        return FeignException.errorStatus("sendTx", Response.builder().status(status).reason("r")
                .request(peticion()).headers(Map.of()).build());
    }

    private static RetryableException timeout() {
        return new RetryableException(-1, "timeout", Request.HttpMethod.POST, new SocketTimeoutException("t"),
                (Long) null, peticion());
    }

    // ---------------- sendTx: resultados ----------------

    @Test
    void envio_conCodigoDeExito_esExitosoYMandaLosDatosCorrectos() {
        when(client.enviarTx(anyString(), any())).thenReturn(respuesta("0", "FOL-1"));

        ResultadoGestoPago resultado = gateway.enviarPago(pago(), producto());

        assertThat(resultado.getTipo()).isEqualTo(Tipo.EXITOSO);
        assertThat(resultado.getFolio()).isEqualTo("FOL-1");

        ArgumentCaptor<GestoPagoTxRequest> cuerpo = ArgumentCaptor.forClass(GestoPagoTxRequest.class);
        verify(client).enviarTx(eq("Bearer TOKEN-SECRETO"), cuerpo.capture());
        assertThat(cuerpo.getValue().getIdProd()).isEqualTo(1);
        assertThat(cuerpo.getValue().getIdUser()).isEqualTo(83);
        assertThat(cuerpo.getValue().getTelefonoReferencia()).isEqualTo("ABC12345");
        assertThat(cuerpo.getValue().getMonto()).isEqualTo("350.00");
        assertThat(cuerpo.getValue().getIdTransaccion()).isEqualTo("ref0001");
        assertThat(cuerpo.getValue().getHoraLocal()).matches("^\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}$");
    }

    @Test
    void envio_conCodigoDeError_esRechazadoYSePuedeDevolverElSaldo() {
        when(client.enviarTx(anyString(), any())).thenReturn(respuesta("5", null));

        ResultadoGestoPago resultado = gateway.enviarPago(pago(), producto());

        assertThat(resultado.getTipo()).isEqualTo(Tipo.RECHAZADO);
        assertThat(resultado.getCodigo()).isEqualTo("5");
        assertThat(resultado.getError()).isEqualTo(CatalogoError.PAGO_RECHAZADO);
    }

    @Test
    void envio_conCodigoDeEnProceso_esIndeterminado() {
        when(client.enviarTx(anyString(), any())).thenReturn(respuesta("99", null));
        assertThat(gateway.enviarPago(pago(), producto()).getTipo()).isEqualTo(Tipo.INDETERMINADO);
    }

    @Test
    void envio_conRespuestaVaciaOSinCodigo_esIndeterminado() {
        when(client.enviarTx(anyString(), any())).thenReturn(null);
        assertThat(gateway.enviarPago(pago(), producto()).getTipo()).isEqualTo(Tipo.INDETERMINADO);

        when(client.enviarTx(anyString(), any())).thenReturn(respuesta(" ", null));
        assertThat(gateway.enviarPago(pago(), producto()).getTipo()).isEqualTo(Tipo.INDETERMINADO);
    }

    // ---------------- sendTx: errores de red / HTTP ----------------

    @Test
    void envio_conTimeout_esIndeterminadoYNuncaRechazado() {
        when(client.enviarTx(anyString(), any())).thenThrow(timeout());

        ResultadoGestoPago resultado = gateway.enviarPago(pago(), producto());

        assertThat(resultado.getTipo()).isEqualTo(Tipo.INDETERMINADO);
        assertThat(resultado.getCodigo()).isEqualTo("SIN_RESPUESTA");
        verify(client, times(1)).enviarTx(anyString(), any());   // jamás se reenvía solo
    }

    @Test
    void envio_conError5xx_esIndeterminado() {
        when(client.enviarTx(anyString(), any())).thenThrow(http(500));
        ResultadoGestoPago resultado = gateway.enviarPago(pago(), producto());
        assertThat(resultado.getTipo()).isEqualTo(Tipo.INDETERMINADO);
        assertThat(resultado.getCodigo()).isEqualTo("HTTP_500");
    }

    @Test
    void envio_con408Y409_sonIndeterminados() {
        doThrow(http(408)).when(client).enviarTx(anyString(), any());
        assertThat(gateway.enviarPago(pago(), producto()).getTipo()).isEqualTo(Tipo.INDETERMINADO);

        doThrow(http(409)).when(client).enviarTx(anyString(), any());
        assertThat(gateway.enviarPago(pago(), producto()).getTipo()).isEqualTo(Tipo.INDETERMINADO);
    }

    @Test
    void envio_con400_esRechazadoSinProcesar() {
        when(client.enviarTx(anyString(), any())).thenThrow(http(400));
        ResultadoGestoPago resultado = gateway.enviarPago(pago(), producto());
        assertThat(resultado.getTipo()).isEqualTo(Tipo.RECHAZADO);
        assertThat(resultado.getError()).isEqualTo(CatalogoError.PAGO_RECHAZADO);
    }

    @Test
    void envio_con429_esRechazadoPorNoDisponible() {
        when(client.enviarTx(anyString(), any())).thenThrow(http(429));
        ResultadoGestoPago resultado = gateway.enviarPago(pago(), producto());
        assertThat(resultado.getTipo()).isEqualTo(Tipo.RECHAZADO);
        assertThat(resultado.getError()).isEqualTo(CatalogoError.GESTOPAGO_NO_DISPONIBLE);
    }

    @Test
    void envio_conErrorInesperado_esIndeterminado() {
        when(client.enviarTx(anyString(), any())).thenThrow(new IllegalStateException("algo raro"));
        assertThat(gateway.enviarPago(pago(), producto()).getTipo()).isEqualTo(Tipo.INDETERMINADO);
    }

    // ---------------- token ----------------

    @Test
    void envio_con401_renuevaElTokenYReintentaUnaVez() {
        when(client.enviarTx(anyString(), any())).thenThrow(http(401)).thenReturn(respuesta("0", "FOL-2"));

        ResultadoGestoPago resultado = gateway.enviarPago(pago(), producto());

        assertThat(resultado.getTipo()).isEqualTo(Tipo.EXITOSO);
        verify(tokenService).renovarToken();
        verify(client, times(2)).enviarTx(anyString(), any());
    }

    @Test
    void envio_con401DosVeces_esRechazadoPorToken() {
        when(client.enviarTx(anyString(), any())).thenThrow(http(401));

        ResultadoGestoPago resultado = gateway.enviarPago(pago(), producto());

        assertThat(resultado.getTipo()).isEqualTo(Tipo.RECHAZADO);
        assertThat(resultado.getError()).isEqualTo(CatalogoError.GESTOPAGO_SIN_TOKEN);
        verify(client, times(2)).enviarTx(anyString(), any());
    }

    @Test
    void envio_sinTokenDisponible_noSaleNadaHaciaElProveedor() {
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.empty());

        ResultadoGestoPago resultado = gateway.enviarPago(pago(), producto());

        assertThat(resultado.getTipo()).isEqualTo(Tipo.RECHAZADO);
        assertThat(resultado.getError()).isEqualTo(CatalogoError.GESTOPAGO_SIN_TOKEN);
        verify(client, never()).enviarTx(anyString(), any());
    }

    // ---------------- consulta (conciliación) ----------------

    @Test
    void consulta_conCodigoDeExito_confirmaElPago() {
        when(client.consultarTx(anyString(), any())).thenReturn(respuesta("0", "FOL-3"));

        ResultadoGestoPago resultado = gateway.consultarPago(pago());

        assertThat(resultado.getTipo()).isEqualTo(Tipo.EXITOSO);
        assertThat(resultado.getFolio()).isEqualTo("FOL-3");
        ArgumentCaptor<GestoPagoConsultaRequest> cuerpo = ArgumentCaptor.forClass(GestoPagoConsultaRequest.class);
        verify(client).consultarTx(eq("Bearer TOKEN-SECRETO"), cuerpo.capture());
        assertThat(cuerpo.getValue().getIdTransaccion()).isEqualTo("ref0001");
    }

    @Test
    void consulta_conCodigoDeNoAplicada_esRechazado() {
        when(client.consultarTx(anyString(), any())).thenReturn(respuesta("10", null));
        assertThat(gateway.consultarPago(pago()).getTipo()).isEqualTo(Tipo.RECHAZADO);
    }

    @Test
    void consulta_conCodigoDesconocido_sigueIndeterminado() {
        when(client.consultarTx(anyString(), any())).thenReturn(respuesta("77", null));
        assertThat(gateway.consultarPago(pago()).getTipo()).isEqualTo(Tipo.INDETERMINADO);
    }

    @Test
    void consulta_conErroresDeRedOHttp_nuncaEsRechazado() {
        doThrow(http(404)).when(client).consultarTx(anyString(), any());
        assertThat(gateway.consultarPago(pago()).getTipo()).isEqualTo(Tipo.INDETERMINADO);

        doThrow(http(400)).when(client).consultarTx(anyString(), any());
        assertThat(gateway.consultarPago(pago()).getTipo()).isEqualTo(Tipo.INDETERMINADO);

        doThrow(timeout()).when(client).consultarTx(anyString(), any());
        assertThat(gateway.consultarPago(pago()).getTipo()).isEqualTo(Tipo.INDETERMINADO);
    }

    @Test
    void consulta_sinToken_sigueIndeterminado() {
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.empty());
        assertThat(gateway.consultarPago(pago()).getTipo()).isEqualTo(Tipo.INDETERMINADO);
        verify(client, never()).consultarTx(anyString(), any());
    }

    // ---------------- transporte seguro ----------------

    @Test
    void urlSinHttps_noSeAcepta() {
        ReflectionTestUtils.setField(gateway, "url", "http://gestopago.portalventas.net");
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(gateway, "validarTransporteSeguro"))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void urlHttpsOLocalhost_seAcepta() {
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(gateway, "validarTransporteSeguro"))
                .doesNotThrowAnyException();
        ReflectionTestUtils.setField(gateway, "url", "http://localhost:9000");
        assertThatCode(() -> ReflectionTestUtils.invokeMethod(gateway, "validarTransporteSeguro"))
                .doesNotThrowAnyException();
    }
}
