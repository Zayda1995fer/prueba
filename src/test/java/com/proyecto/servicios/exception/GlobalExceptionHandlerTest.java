package com.proyecto.servicios.exception;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.config.WebConfig;
import com.proyecto.servicios.controller.ClienteController;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.service.ClienteService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.beanvalidation.LocalValidatorFactoryBean;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Prueba de punta a punta de la API (controller + validaciones + manejador
 * de errores) sin base de datos: el servicio se simula con Mockito.
 */
@ExtendWith(MockitoExtension.class)
class GlobalExceptionHandlerTest {

    @Mock
    private ClienteService clienteService;

    private final ObjectMapper escritor = new ObjectMapper();
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        ObjectMapper estricto = WebConfig.crearMapperEstricto(new ObjectMapper().registerModule(new JavaTimeModule()));
        LocalValidatorFactoryBean validador = new LocalValidatorFactoryBean();
        validador.afterPropertiesSet();
        mvc = MockMvcBuilders.standaloneSetup(new ClienteController(clienteService))
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(new MappingJackson2HttpMessageConverter(estricto))
                .setValidator(validador)
                .build();
    }

    private Map<String, Object> cuerpoValido() {
        Map<String, Object> domicilio = new LinkedHashMap<>();
        domicilio.put("calle", "Reforma");
        domicilio.put("numeroExterior", "100");
        domicilio.put("colonia", "Centro");
        domicilio.put("municipio", "Cuauhtémoc");
        domicilio.put("estado", "Ciudad de México");
        domicilio.put("codigoPostal", "06000");
        domicilio.put("pais", "México");

        Map<String, Object> c = new LinkedHashMap<>();
        c.put("nombre", "Juan");
        c.put("apellidoPaterno", "Garcia");
        c.put("apellidoMaterno", "Lopez");
        c.put("fechaNacimiento", "1990-01-01");
        c.put("curp", "GALJ900101HDFRPN01");
        c.put("rfc", "GALJ900101A01");
        c.put("sexo", "H");
        c.put("nacionalidad", List.of(Map.of("id", 1, "nombre", "Mexicana")));
        c.put("estadoCivil", "Soltero(a)");
        c.put("correoElectronico", "juan.garcia@correo.com");
        c.put("telefonoMovil", 5512345678L);
        c.put("domicilio", domicilio);
        c.put("ocupacion", "Ingeniero");
        c.put("empresa", "ACME");
        c.put("ingresoMensual", 15000);
        return c;
    }

    private org.springframework.test.web.servlet.ResultActions enviar(Object cuerpo) throws Exception {
        String json = cuerpo instanceof String s ? s : escritor.writeValueAsString(cuerpo);
        return mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void solicitudValida_respondeCreated() throws Exception {
        when(clienteService.registrarCliente(any())).thenReturn(new ClienteResponse());

        enviar(cuerpoValido())
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.codigo").value(201))
                .andExpect(jsonPath("$.message").value("Cliente registrado correctamente"))
                .andExpect(jsonPath("$.data").exists());
    }

    @Test
    void telefonoConCaracteresEspeciales_respondeUnSoloErrorDeEseCampo() throws Exception {
        Map<String, Object> cuerpo = cuerpoValido();
        cuerpo.put("telefonoMovil", "55#$%&@");

        enviar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.message").value("Error de validación"))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("telefonoMovil"))
                .andExpect(jsonPath("$.errors[0].message",
                        containsString("solo debe contener números, con formato de 10 dígitos y lada válida")));
    }

    @Test
    void variosErrores_seRegresaSoloElPrimeroSegunElOrdenDelRequest() throws Exception {
        // El mismo caso de la captura: nacionalidad faltante + estado civil inválido + teléfono inválido
        Map<String, Object> cuerpo = cuerpoValido();
        cuerpo.remove("nacionalidad");
        cuerpo.put("estadoCivil", "Otro");
        cuerpo.put("telefonoMovil", 123L);

        enviar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("nacionalidad"));

        // Al corregir ese campo, aparece el error del siguiente
        cuerpo.put("nacionalidad", List.of(Map.of("id", 1, "nombre", "Mexicana")));
        enviar(cuerpo)
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("estadoCivil"));

        cuerpo.put("estadoCivil", "Soltero(a)");
        enviar(cuerpo)
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("telefonoMovil"));
    }

    @Test
    void errorEnDomicilio_indicaLaRutaCompletaDelCampo() throws Exception {
        Map<String, Object> cuerpo = cuerpoValido();
        @SuppressWarnings("unchecked")
        Map<String, Object> domicilio = (Map<String, Object>) cuerpo.get("domicilio");
        domicilio.put("codigoPostal", "0600");

        enviar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("domicilio.codigoPostal"))
                .andExpect(jsonPath("$.errors[0].message").value("El código postal debe contener exactamente 5 dígitos"));
    }

    @Test
    void campoNoPermitido_seRechazaParaEvitarMassAssignment() throws Exception {
        Map<String, Object> cuerpo = cuerpoValido();
        cuerpo.put("activo", true);

        enviar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("activo"))
                .andExpect(jsonPath("$.errors[0].message", containsString("no está permitido")));
    }

    @Test
    void ingresoMensualComoTexto_seRechaza() throws Exception {
        Map<String, Object> cuerpo = cuerpoValido();
        cuerpo.put("ingresoMensual", "quince mil");

        enviar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("ingresoMensual"))
                .andExpect(jsonPath("$.errors[0].message", containsString("número válido")));
    }

    @Test
    void fechaImposible_seRechaza() throws Exception {
        Map<String, Object> cuerpo = cuerpoValido();
        cuerpo.put("fechaNacimiento", "2024-02-31");

        enviar(cuerpo)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("fechaNacimiento"))
                .andExpect(jsonPath("$.errors[0].message", containsString("AAAA-MM-DD")));
    }

    @Test
    void jsonMalFormado_respondeBadRequest() throws Exception {
        enviar("{\"nombre\":")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString("no es un JSON válido")));
    }

    @Test
    void sinCuerpo_respondeBadRequest() throws Exception {
        mvc.perform(post("/clientes").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void contentTypeIncorrecto_respondeUnsupportedMediaType() throws Exception {
        mvc.perform(post("/clientes").contentType(MediaType.TEXT_PLAIN).content("hola"))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", containsString("application/json")));
    }

    @Test
    void duplicado_respondeConflictIndicandoElCampo() throws Exception {
        when(clienteService.registrarCliente(any())).thenThrow(new NegocioException(CatalogoError.CURP_DUPLICADA, "GALJ900101HDFRPN01"));

        enviar(cuerpoValido())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.field").value("curp"))
                .andExpect(jsonPath("$.message", containsString("GALJ900101HDFRPN01")));
    }

    private org.springframework.test.web.servlet.ResultActions consultar(String json) throws Exception {
        return mvc.perform(post("/clientes/consultar").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    @Test
    void consultaConFiltrosCombinados_respondeOkConLaEstructuraUniforme() throws Exception {
        when(clienteService.consultarClientes(any())).thenReturn(List.of(new ClienteResponse()));

        consultar("{\"nombre\":\"Juan\",\"activo\":true,\"fechaCreacionDesde\":\"2026-01-01\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.codigo").value(200))
                .andExpect(jsonPath("$.message").value("Consulta realizada correctamente"))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void consultaSinFiltros_seAceptaConBodyVacio() throws Exception {
        when(clienteService.consultarClientes(any())).thenReturn(List.of());

        consultar("{}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(0));
    }

    @Test
    void clienteNoEncontrado_respondeNotFound() throws Exception {
        when(clienteService.consultarClientes(any())).thenThrow(new NegocioException(CatalogoError.CLIENTE_NO_ENCONTRADO, "id 5"));

        consultar("{\"id\":5}")
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value(404));
    }

    @Test
    void idQueNoEsNumero_respondeBadRequest() throws Exception {
        consultar("{\"id\":\"abc\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"))
                .andExpect(jsonPath("$.errors[0].message", containsString("número entero")));
    }

    @Test
    void consultaConCurpInvalida_indicaElCampo() throws Exception {
        consultar("{\"curp\":\"123\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("curp"));
    }

    @Test
    void consultaConCampoNoPermitido_seRechaza() throws Exception {
        consultar("{\"saldo\":100}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].message", containsString("no está permitido")));
    }

    @Test
    void consultaSinCuerpo_respondeBadRequest() throws Exception {
        mvc.perform(post("/clientes/consultar").contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
    }

    @Test
    void actualizarSinId_indicaQueElIdEsObligatorio() throws Exception {
        mvc.perform(put("/clientes").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors[0].field").value("id"))
                .andExpect(jsonPath("$.errors[0].message", containsString("obligatorio")));
    }

    @Test
    void errorInesperado_respondeInternalServerErrorSinDetallesInternos() throws Exception {
        when(clienteService.registrarCliente(any())).thenThrow(new IllegalStateException("boom: detalle interno SQL"));

        enviar(cuerpoValido())
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.message", not(containsString("boom"))))
                .andExpect(jsonPath("$.message", not(containsString("SQL"))));
    }
}
