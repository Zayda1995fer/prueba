package com.proyecto.servicios.integracion;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.config.SesionInterceptor;
import com.proyecto.servicios.config.WebConfig;
import com.proyecto.servicios.controller.PagoController;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.PagoRepository;
import com.proyecto.servicios.repositorys.onboarding.ProductoPagoRepository;
import com.proyecto.servicios.service.DobleFactorService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.LoginService;
import com.proyecto.servicios.service.Impl.ConciliacionServiceImpl;
import com.proyecto.servicios.service.Impl.GestoPagoGatewayImpl;
import com.proyecto.servicios.service.Impl.PagoServiceImpl;
import com.proyecto.servicios.service.Impl.PagoTransaccionServiceImpl;
import com.proyecto.servicios.service.NotificacionService;
import com.proyecto.servicios.validation.ValidadorPago;
import feign.Request;
import feign.RetryableException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.math.BigDecimal;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de integración de las capas: Controller + servicios + gateway reales, con repositorios en
 * memoria y un proveedor simulado. Comprueba el flujo completo de dinero: descuento, confirmación,
 * rechazo, timeout, conciliación e idempotencia.
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PagoFlujoCompletoTest {

    private static final String SOLICITUD_1 = "3f2b8c1e-9a4d-4c7e-8b21-5d6e7f8a9b0c";
    private static final String SOLICITUD_2 = "11111111-2222-4333-8444-555555555555";
    private static final String TOKEN_SESION = "aaaaaaaa-bbbb-4ccc-8ddd-eeeeeeeeeeee";

    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private ProductoPagoRepository productoRepository;
    @Mock
    private DobleFactorService dobleFactorService;
    @Mock
    private NotificacionService notificacionService;
    @Mock
    private GestoPagoTxClient client;
    @Mock
    private GestoPagoTokenService tokenService;
    @Mock
    private LoginService loginService;

    // "Base de datos" en memoria
    private final Map<Integer, Pago> pagos = new LinkedHashMap<>();
    private final AtomicInteger secuencia = new AtomicInteger(100);
    private Cuenta cuenta;

    private final ObjectMapper escritor = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mvc;
    private ConciliacionServiceImpl conciliacion;

    @BeforeEach
    void setUp() {
        cuenta = new Cuenta();
        cuenta.setId(9);
        cuenta.setClienteId(5);
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setSaldo(new BigDecimal("2000.00"));
        cuenta.setEstatus(Cuenta.ESTATUS_ACTIVA);

        Cliente cliente = new Cliente();
        cliente.setId(5);
        cliente.setNombre("Ana");
        cliente.setCorreoElectronico("ana@correo.com");
        cliente.setActivo(true);

        ProductoPago luz = new ProductoPago();
        luz.setIdProducto(1);
        luz.setIdServicio(10);
        luz.setNombre("Pago de luz (CFE)");
        luz.setTipoDestino(ProductoPago.DESTINO_REFERENCIA);
        luz.setMontoMinimo(new BigDecimal("10.00"));
        luz.setMontoMaximo(new BigDecimal("10000.00"));
        luz.setActivo(true);

        // Sesión del cliente 5, titular de la cuenta
        when(loginService.validarSesion(TOKEN_SESION)).thenReturn(5);
        when(cuentaRepository.buscarParaPago("1234567890")).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.buscarPorIdParaActualizar(9)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.findById(9)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));
        when(clienteRepository.findById(5)).thenReturn(Optional.of(cliente));
        when(productoRepository.findById(1)).thenReturn(Optional.of(luz));

        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> {
            Pago p = inv.getArgument(0);
            if (p.getId() == null) {
                p.setId(secuencia.incrementAndGet());
            }
            pagos.put(p.getId(), p);
            return p;
        });
        when(pagoRepository.findByIdSolicitud(anyString())).thenAnswer(inv ->
                pagos.values().stream().filter(p -> p.getIdSolicitud().equals(inv.getArgument(0))).findFirst());
        when(pagoRepository.buscarParaActualizar(anyInt())).thenAnswer(inv ->
                Optional.ofNullable(pagos.get(inv.<Integer>getArgument(0))));
        when(pagoRepository.contarSimilares(any(), any(), any(), any(), anyCollection(), any())).thenAnswer(inv -> {
            Collection<String> estatus = inv.getArgument(4);
            return pagos.values().stream()
                    .filter(p -> p.getCuentaId().equals(inv.getArgument(0))
                            && p.getIdProducto().equals(inv.getArgument(1))
                            && p.getDestino().equals(inv.getArgument(2))
                            && p.getMonto().compareTo(inv.<BigDecimal>getArgument(3)) == 0
                            && estatus.contains(p.getEstatus()))
                    .count();
        });
        when(pagoRepository.buscarPorConciliar(anyCollection(), any(), anyInt(), any())).thenAnswer(inv -> {
            Collection<String> estatus = inv.getArgument(0);
            int max = inv.getArgument(2);
            return new ArrayList<>(pagos.values().stream()
                    .filter(p -> estatus.contains(p.getEstatus()) && p.getIntentosConciliacion() < max).toList());
        });

        GestoPagoToken token = new GestoPagoToken();
        token.setToken("TOKEN-SECRETO");
        when(tokenService.obtenerTokenActivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(token));

        // Capas reales
        GestoPagoGatewayImpl gateway = new GestoPagoGatewayImpl(client, tokenService);
        ReflectionTestUtils.setField(gateway, "url", "https://gestopago.portalventas.net");
        ReflectionTestUtils.setField(gateway, "idDistribuidor", 83);
        ReflectionTestUtils.setField(gateway, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(gateway, "idUser", 83);
        ReflectionTestUtils.setField(gateway, "upc", "");
        ReflectionTestUtils.setField(gateway, "zonaHoraria", "America/Mexico_City");
        ReflectionTestUtils.setField(gateway, "codigoExito", "0");
        ReflectionTestUtils.setField(gateway, "codigosPendientes", "");
        ReflectionTestUtils.setField(gateway, "codigosNoAplicada", "10");

        PagoTransaccionServiceImpl transacciones = new PagoTransaccionServiceImpl(clienteRepository, cuentaRepository,
                pagoRepository, productoRepository, dobleFactorService, notificacionService);
        ReflectionTestUtils.setField(transacciones, "ventanaDuplicadosSegundos", 60);
        ReflectionTestUtils.setField(transacciones, "maxIntentosConciliacion", 3);

        PagoServiceImpl pagoService = new PagoServiceImpl(new ValidadorPago(productoRepository), transacciones,
                gateway, pagoRepository, productoRepository, cuentaRepository);

        conciliacion = new ConciliacionServiceImpl(pagoRepository, gateway, transacciones);
        ReflectionTestUtils.setField(conciliacion, "activa", true);
        ReflectionTestUtils.setField(conciliacion, "esperaSegundos", 0);
        ReflectionTestUtils.setField(conciliacion, "maxIntentos", 3);
        ReflectionTestUtils.setField(conciliacion, "tamanoLote", 20);

        ObjectMapper estricto = WebConfig.crearMapperEstricto(new ObjectMapper().registerModule(new JavaTimeModule()));
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new PagoController(pagoService, dobleFactorService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(estricto))
                .setValidator(validador)
                .addInterceptors(new SesionInterceptor(loginService))
                .defaultRequest(post("/").header(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN_SESION))
                .build();
    }

    // ---------------- apoyo ----------------

    private Map<String, Object> cuerpo(String solicitud, String monto) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idSolicitud", solicitud);
        m.put("numeroCuenta", "1234567890");
        m.put("idUser", 5);
        m.put("idServicio", 10);
        m.put("idProducto", 1);
        m.put("monto", new BigDecimal(monto));
        m.put("referencia", "ABC12345");
        m.put("codigo2fa", "123456");
        return m;
    }

    private ResultActions pagar(String solicitud, String monto) throws Exception {
        return mvc.perform(post("/pagos").contentType(MediaType.APPLICATION_JSON)
                .content(escritor.writeValueAsString(cuerpo(solicitud, monto))));
    }

    private ResultActions consultar(String solicitud) throws Exception {
        return mvc.perform(post("/pagos/consultar").contentType(MediaType.APPLICATION_JSON)
                .content("{\"idSolicitud\":\"" + solicitud + "\",\"idUser\":5}"));
    }

    private GestoPagoTxResponse respuesta(String codigo, String folio) {
        GestoPagoTxResponse r = new GestoPagoTxResponse();
        r.setCodigo(codigo);
        r.setFolioTransaccion(folio);
        return r;
    }

    private RetryableException timeout() {
        Request peticion = Request.create(Request.HttpMethod.POST, "http://localhost/x", Map.of(), null,
                StandardCharsets.UTF_8, null);
        return new RetryableException(-1, "timeout", Request.HttpMethod.POST, new SocketTimeoutException("t"),
                (Long) null, peticion);
    }

    private Pago unicoPago() {
        assertThat(pagos).hasSize(1);
        return pagos.values().iterator().next();
    }

    // ---------------- escenarios ----------------

    @Test
    void sinSesion_respondeUnauthorizedYNoTocaElSaldo() throws Exception {
        mvc.perform(post("/pagos").header(HttpHeaders.AUTHORIZATION, "")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(escritor.writeValueAsString(cuerpo(SOLICITUD_1, "350.00"))))
                .andExpect(status().isUnauthorized());

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("2000.00");
        assertThat(pagos).isEmpty();
        verify(client, never()).enviarTx(anyString(), any());
    }

    @Test
    void sesionDeOtroCliente_respondeForbiddenYNoTocaElSaldo() throws Exception {
        // El cliente 6 tiene sesión válida pero intenta pagar con la cuenta del cliente 5
        when(loginService.validarSesion(TOKEN_SESION)).thenReturn(6);

        pagar(SOLICITUD_1, "350.00").andExpect(status().isForbidden());

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("2000.00");
        assertThat(pagos).isEmpty();
        verify(client, never()).enviarTx(anyString(), any());
    }

    @Test
    void pagoExitoso_descuentaElSaldoGuardaElPagoYAvisaAlCliente() throws Exception {
        when(client.enviarTx(anyString(), any())).thenReturn(respuesta("0", "FOL-1"));

        pagar(SOLICITUD_1, "350.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.estatus").value("EXITOSO"))
                .andExpect(jsonPath("$.data.folioTransaccion").value("FOL-1"))
                .andExpect(jsonPath("$.data.saldoActual").value(1650.00))
                .andExpect(jsonPath("$.data.destino").value("****2345"));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");
        assertThat(unicoPago().getEstatus()).isEqualTo(Pago.ESTATUS_EXITOSO);
        verify(notificacionService).notificarPago(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void laMismaSolicitudDosVeces_cobraYLlamaAlProveedorUnaSolaVez() throws Exception {
        when(client.enviarTx(anyString(), any())).thenReturn(respuesta("0", "FOL-1"));

        pagar(SOLICITUD_1, "350.00").andExpect(status().isCreated());
        pagar(SOLICITUD_1, "350.00")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.folioTransaccion").value("FOL-1"));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");
        assertThat(pagos).hasSize(1);
        verify(client, times(1)).enviarTx(anyString(), any());
    }

    @Test
    void laMismaSolicitudConOtroMonto_esConflictoYNoCobra() throws Exception {
        when(client.enviarTx(anyString(), any())).thenReturn(respuesta("0", "FOL-1"));
        pagar(SOLICITUD_1, "350.00").andExpect(status().isCreated());

        pagar(SOLICITUD_1, "900.00").andExpect(status().isConflict());

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");
        verify(client, times(1)).enviarTx(anyString(), any());
    }

    @Test
    void pagoIgualConOtroIdDeSolicitud_sePideComoDuplicadoYNoCobra() throws Exception {
        when(client.enviarTx(anyString(), any())).thenReturn(respuesta("0", "FOL-1"));
        pagar(SOLICITUD_1, "350.00").andExpect(status().isCreated());

        pagar(SOLICITUD_2, "350.00").andExpect(status().isConflict());

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");
        verify(client, times(1)).enviarTx(anyString(), any());
    }

    @Test
    void rechazoDelProveedor_devuelveElSaldoYAvisaQueNoHuboCargo() throws Exception {
        when(client.enviarTx(anyString(), any())).thenReturn(respuesta("5", null));

        pagar(SOLICITUD_1, "350.00")
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("No se realizó ningún cargo")));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("2000.00");
        assertThat(unicoPago().getEstatus()).isEqualTo(Pago.ESTATUS_RECHAZADO);
        verify(notificacionService, never()).notificarPago(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void saldoInsuficiente_noLlegaAlProveedor() throws Exception {
        pagar(SOLICITUD_1, "2500.00").andExpect(status().isUnprocessableEntity());

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("2000.00");
        assertThat(pagos).isEmpty();
        verify(client, never()).enviarTx(anyString(), any());
    }

    @Test
    void montoFueraDelCatalogo_noLlegaAlProveedor() throws Exception {
        pagar(SOLICITUD_1, "10001.00").andExpect(status().isBadRequest());

        assertThat(pagos).isEmpty();
        verify(client, never()).enviarTx(anyString(), any());
    }

    @Test
    void dobleFactorIncorrecto_noDescuentaNiLlegaAlProveedor() throws Exception {
        doThrow(new NegocioException(CatalogoError.DOBLE_FACTOR_INVALIDO))
                .when(dobleFactorService).validar(5, 9, "123456");

        pagar(SOLICITUD_1, "350.00").andExpect(status().isUnauthorized());

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("2000.00");
        assertThat(pagos).isEmpty();
        verify(client, never()).enviarTx(anyString(), any());
    }

    @Test
    void timeout_dejaElSaldoRetenido_yLaConciliacionLoConfirmaSinReenviar() throws Exception {
        when(client.enviarTx(anyString(), any())).thenThrow(timeout());

        pagar(SOLICITUD_1, "350.00")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.estatus").value("POR_CONCILIAR"))
                .andExpect(jsonPath("$.data.saldoActual").value(1650.00));
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");   // NO se asumió que falló

        // El proveedor sí lo había aplicado
        when(client.consultarTx(anyString(), any())).thenReturn(respuesta("0", "FOL-9"));
        assertThat(conciliacion.conciliarPendientes()).isEqualTo(1);

        assertThat(unicoPago().getEstatus()).isEqualTo(Pago.ESTATUS_EXITOSO);
        assertThat(unicoPago().getFolioTransaccion()).isEqualTo("FOL-9");
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");
        verify(client, times(1)).enviarTx(anyString(), any());   // jamás se reenvió
        consultar(SOLICITUD_1).andExpect(status().isOk()).andExpect(jsonPath("$.data.estatus").value("EXITOSO"));
    }

    @Test
    void timeout_yElProveedorNoLoAplico_laConciliacionDevuelveElSaldoUnaSolaVez() throws Exception {
        when(client.enviarTx(anyString(), any())).thenThrow(timeout());
        pagar(SOLICITUD_1, "350.00").andExpect(status().isAccepted());

        when(client.consultarTx(anyString(), any())).thenReturn(respuesta("10", null));
        conciliacion.conciliarPendientes();
        conciliacion.conciliarPendientes();   // una segunda corrida no debe devolver otra vez

        assertThat(unicoPago().getEstatus()).isEqualTo(Pago.ESTATUS_RECHAZADO);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("2000.00");
        verify(client, times(1)).enviarTx(anyString(), any());
    }

    @Test
    void timeout_yLaConsultaTampocoResponde_pasaARevisionManualConElSaldoRetenido() throws Exception {
        when(client.enviarTx(anyString(), any())).thenThrow(timeout());
        pagar(SOLICITUD_1, "350.00").andExpect(status().isAccepted());
        when(client.consultarTx(anyString(), any())).thenThrow(timeout());

        conciliacion.conciliarPendientes();
        conciliacion.conciliarPendientes();
        conciliacion.conciliarPendientes();
        conciliacion.conciliarPendientes();   // ya está en revisión manual: no se vuelve a consultar

        assertThat(unicoPago().getEstatus()).isEqualTo(Pago.ESTATUS_REVISION_MANUAL);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");
        verify(client, times(3)).consultarTx(anyString(), any());
        verify(client, times(1)).enviarTx(anyString(), any());
    }

    @Test
    void reintentoDelClienteTrasUnTimeout_noCobraDeNuevo() throws Exception {
        when(client.enviarTx(anyString(), any())).thenThrow(timeout());
        pagar(SOLICITUD_1, "350.00").andExpect(status().isAccepted());

        // El cliente reintenta con la misma solicitud
        pagar(SOLICITUD_1, "350.00")
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.data.estatus").value("POR_CONCILIAR"));

        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");
        verify(client, times(1)).enviarTx(anyString(), any());
    }
}
