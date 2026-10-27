package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.config.WebConfig;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.onboarding.Codigo2faResponse;
import com.proyecto.servicios.model.onboarding.PagoResponse;
import com.proyecto.servicios.service.DobleFactorService;
import com.proyecto.servicios.service.PagoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PagoControllerTest {

    private static final String ID_SOLICITUD = "3f2b8c1e-9a4d-4c7e-8b21-5d6e7f8a9b0c";

    @Mock
    private PagoService pagoService;
    @Mock
    private DobleFactorService dobleFactorService;

    private final ObjectMapper escritor = new ObjectMapper().registerModule(new JavaTimeModule());
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ObjectMapper estricto = WebConfig.crearMapperEstricto(new ObjectMapper().registerModule(new JavaTimeModule()));
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new PagoController(pagoService, dobleFactorService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(estricto))
                .setValidator(validador)
                .build();
    }

    // ---------------- datos de prueba ----------------

    private Map<String, Object> cuerpoLuz() {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("idSolicitud", ID_SOLICITUD);
        m.put("numeroCuenta", "1234567890");
        m.put("idUser", 5);
        m.put("idServicio", 10);
        m.put("idProducto", 1);
        m.put("monto", new BigDecimal("350.00"));
        m.put("referencia", "ABC12345");
        m.put("codigo2fa", "123456");
        return m;
    }

    private ResultActions pagar(Map<String, Object> cuerpo) throws Exception {
        return mvc.perform(post("/pagos").contentType(MediaType.APPLICATION_JSON)
                .content(escritor.writeValueAsString(cuerpo)));
    }

    private PagoResponse respuesta(String estatus) {
        PagoResponse r = new PagoResponse();
        r.setIdSolicitud(ID_SOLICITUD);
        r.setEstatus(estatus);
        r.setMonto(new BigDecimal("350.00"));
        return r;
    }

    // ---------------- respuestas correctas ----------------

    @Test
    void pagoConfirmado_respondeCreated() throws Exception {
        when(pagoService.pagar(any())).thenReturn(respuesta("EXITOSO"));

        pagar(cuerpoLuz())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Pago realizado correctamente"))
                .andExpect(jsonPath("$.data.estatus").value("EXITOSO"));
    }

    @Test
    void pagoPorConciliar_respondeAcceptedYExplicaQueElSaldoQuedaRetenido() throws Exception {
        when(pagoService.pagar(any())).thenReturn(respuesta("POR_CONCILIAR"));

        pagar(cuerpoLuz())
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value(containsString("proceso de confirmación")))
                .andExpect(jsonPath("$.data.estatus").value("POR_CONCILIAR"));
    }

    @Test
    void consultar_respondeOk() throws Exception {
        when(pagoService.consultarPago(any())).thenReturn(respuesta("EXITOSO"));

        mvc.perform(post("/pagos/consultar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSolicitud\":\"" + ID_SOLICITUD + "\",\"idUser\":5}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.estatus").value("EXITOSO"));
    }

    @Test
    void solicitarCodigo2fa_respondeCreated() throws Exception {
        Codigo2faResponse response = new Codigo2faResponse();
        response.setCodigo("123456");
        response.setVigenciaMinutos(5);
        when(dobleFactorService.solicitar(any())).thenReturn(response);

        mvc.perform(post("/pagos/2fa").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"numeroCuenta\":\"1234567890\",\"idUser\":5}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Código 2FA generado correctamente"))
                .andExpect(jsonPath("$.data.vigenciaMinutos").value(5));
    }

    // ---------------- validación de la petición ----------------

    @Test
    void sinIdDeSolicitud_indicaElCampoYNoLlamaAlServicio() throws Exception {
        Map<String, Object> cuerpo = cuerpoLuz();
        cuerpo.remove("idSolicitud");

        pagar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].field").value("idSolicitud"));
        verifyNoInteractions(pagoService);
    }

    @Test
    void idDeSolicitudQueNoEsUuid_seRechaza() throws Exception {
        Map<String, Object> cuerpo = cuerpoLuz();
        cuerpo.put("idSolicitud", "123");

        pagar(cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("idSolicitud"));
    }

    @Test
    void montoConTresDecimales_seRechazaComoManipulado() throws Exception {
        Map<String, Object> cuerpo = cuerpoLuz();
        cuerpo.put("monto", new BigDecimal("350.001"));

        pagar(cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("monto"));
        verifyNoInteractions(pagoService);
    }

    @Test
    void montoCeroONegativo_seRechaza() throws Exception {
        Map<String, Object> cuerpo = cuerpoLuz();
        cuerpo.put("monto", new BigDecimal("-5.00"));

        pagar(cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("monto"));
    }

    @Test
    void montoComoTexto_seRechaza() throws Exception {
        Map<String, Object> cuerpo = cuerpoLuz();
        cuerpo.put("monto", "mucho");

        pagar(cuerpo).andExpect(status().isBadRequest());
        verifyNoInteractions(pagoService);
    }

    @Test
    void referenciaConSimbolos_seRechaza() throws Exception {
        Map<String, Object> cuerpo = cuerpoLuz();
        cuerpo.put("referencia", "AB'; DROP--");

        pagar(cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("referencia"));
    }

    @Test
    void codigo2faQueNoTiene6Digitos_seRechaza() throws Exception {
        Map<String, Object> cuerpo = cuerpoLuz();
        cuerpo.put("codigo2fa", "12");

        pagar(cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("codigo2fa"));
    }

    @Test
    void campoQueNoExisteEnLaPeticion_seRechaza() throws Exception {
        Map<String, Object> cuerpo = cuerpoLuz();
        cuerpo.put("saldoActual", 999999);   // intento de manipular el saldo

        pagar(cuerpo).andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value(containsString("saldoActual")));
        verifyNoInteractions(pagoService);
    }

    @Test
    void contentTypeQueNoEsJson_respondeUnsupportedMediaType() throws Exception {
        mvc.perform(post("/pagos").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void solicitudDe2faSinCuenta_indicaElCampo() throws Exception {
        mvc.perform(post("/pagos/2fa").contentType(MediaType.APPLICATION_JSON).content("{\"idUser\":5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("numeroCuenta"));
        verifyNoInteractions(dobleFactorService);
    }

    @Test
    void consultarSinIdDeUsuario_indicaElCampo() throws Exception {
        mvc.perform(post("/pagos/consultar").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"idSolicitud\":\"" + ID_SOLICITUD + "\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("idUser"));
    }

    // ---------------- cada error del negocio tiene su código HTTP ----------------

    private void servicioLanza(CatalogoError error, Object... args) {
        when(pagoService.pagar(any())).thenThrow(new NegocioException(error, args));
    }

    @Test
    void saldoInsuficiente_respondeUnprocessableEntity() throws Exception {
        servicioLanza(CatalogoError.SALDO_INSUFICIENTE);
        pagar(cuerpoLuz()).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.field").value("monto"));
    }

    @Test
    void pagoDuplicado_respondeConflict() throws Exception {
        servicioLanza(CatalogoError.PAGO_DUPLICADO);
        pagar(cuerpoLuz()).andExpect(status().isConflict());
    }

    @Test
    void idempotenciaConDatosDistintos_respondeConflict() throws Exception {
        servicioLanza(CatalogoError.IDEMPOTENCIA_CONFLICTO);
        pagar(cuerpoLuz()).andExpect(status().isConflict());
    }

    @Test
    void productoInexistente_respondeNotFound() throws Exception {
        servicioLanza(CatalogoError.PRODUCTO_PAGO_NO_ENCONTRADO, 1);
        pagar(cuerpoLuz()).andExpect(status().isNotFound());
    }

    @Test
    void montoFueraDeRango_respondeBadRequest() throws Exception {
        servicioLanza(CatalogoError.MONTO_FUERA_DE_RANGO, "10.00", "100.00");
        pagar(cuerpoLuz()).andExpect(status().isBadRequest()).andExpect(jsonPath("$.field").value("monto"));
    }

    @Test
    void dobleFactorExpiradoOInvalido_respondeUnauthorized() throws Exception {
        servicioLanza(CatalogoError.DOBLE_FACTOR_EXPIRADO);
        pagar(cuerpoLuz()).andExpect(status().isUnauthorized()).andExpect(jsonPath("$.field").value("codigo2fa"));
    }

    @Test
    void dobleFactorBloqueado_respondeTooManyRequests() throws Exception {
        servicioLanza(CatalogoError.DOBLE_FACTOR_BLOQUEADO);
        pagar(cuerpoLuz()).andExpect(status().isTooManyRequests());
    }

    @Test
    void pagoRechazadoPorElProveedor_respondeUnprocessableEntityYAclaraQueNoHuboCargo() throws Exception {
        servicioLanza(CatalogoError.PAGO_RECHAZADO, "5");
        pagar(cuerpoLuz()).andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.message").value(containsString("No se realizó ningún cargo")));
    }

    @Test
    void proveedorSinToken_respondeServiceUnavailable() throws Exception {
        servicioLanza(CatalogoError.GESTOPAGO_SIN_TOKEN);
        pagar(cuerpoLuz()).andExpect(status().isServiceUnavailable());
    }

    @Test
    void proveedorNoDisponible_respondeBadGateway() throws Exception {
        servicioLanza(CatalogoError.GESTOPAGO_NO_DISPONIBLE);
        pagar(cuerpoLuz()).andExpect(status().isBadGateway());
    }

    @Test
    void errorInesperado_respondeInternalErrorSinFiltrarDetallesTecnicos() throws Exception {
        when(pagoService.pagar(any())).thenThrow(new IllegalStateException("jdbc:postgresql://host password=secreto"));

        pagar(cuerpoLuz()).andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.message").value(not(containsString("secreto"))))
                .andExpect(jsonPath("$.message").value(not(containsString("jdbc"))));
    }

    // ---------------- reglas del profesor para los endpoints ----------------

    @Test
    void losEndpointsSoloRecibenElCuerpoJson_sinPathVariableNiRequestParam() {
        List<Method> endpoints = Arrays.stream(PagoController.class.getDeclaredMethods())
                .filter(m -> m.isAnnotationPresent(PostMapping.class)).toList();

        assertThat(endpoints).hasSize(3);   // pagar, consultar y solicitar código 2FA
        for (Method endpoint : endpoints) {
            for (Parameter p : endpoint.getParameters()) {
                assertThat(p.isAnnotationPresent(PathVariable.class)).isFalse();
                assertThat(p.isAnnotationPresent(RequestParam.class)).isFalse();
                assertThat(p.isAnnotationPresent(RequestBody.class)).isTrue();
            }
        }
    }
}
