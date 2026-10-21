package com.proyecto.servicios.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.proyecto.servicios.config.WebConfig;
import com.proyecto.servicios.exception.GlobalExceptionHandler;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ClienteControllerEndpointsTest {

    @Mock
    private ClienteService clienteService;

    private final ObjectMapper escritor = new ObjectMapper().registerModule(new JavaTimeModule());
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

    private Map<String, Object> cuerpoCliente() {
        Map<String, Object> domicilio = new LinkedHashMap<>();
        domicilio.put("calle", "Reforma");
        domicilio.put("numeroExterior", "100");
        domicilio.put("colonia", "Centro");
        domicilio.put("municipio", "Cuauhtemoc");
        domicilio.put("estado", "Jalisco");
        domicilio.put("codigoPostal", "06000");
        domicilio.put("pais", "Mexico");

        Map<String, Object> cliente = new LinkedHashMap<>();
        cliente.put("nombre", "Juan");
        cliente.put("apellidoPaterno", "Garcia");
        cliente.put("apellidoMaterno", "Lopez");
        cliente.put("fechaNacimiento", "1990-01-01");
        cliente.put("curp", "GALJ900101HDFRPN01");
        cliente.put("rfc", "GALJ900101A01");
        cliente.put("sexo", "H");
        cliente.put("nacionalidad", List.of(Map.of("id", 1, "nombre", "Mexicana")));
        cliente.put("estadoCivil", "Soltero(a)");
        cliente.put("correoElectronico", "juan.garcia@correo.com");
        cliente.put("telefonoMovil", 5512345678L);
        cliente.put("domicilio", domicilio);
        cliente.put("ocupacion", "Ingeniero");
        cliente.put("empresa", "ACME");
        cliente.put("ingresoMensual", 15000);
        return cliente;
    }

    private String json(Object body) throws Exception {
        return escritor.writeValueAsString(body);
    }

    @Test
    void registrarCliente_respondeCreatedConRespuestaExitosa() throws Exception {
        when(clienteService.registrarCliente(any())).thenReturn(new ClienteResponse());

        mvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cuerpoCliente())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.codigo").value(201))
                .andExpect(jsonPath("$.message").value("Cliente registrado correctamente"))
                .andExpect(jsonPath("$.data").exists());

        verify(clienteService).registrarCliente(any());
    }

    @Test
    void consultarClientes_respondeOkConLosResultados() throws Exception {
        when(clienteService.consultarClientes(any())).thenReturn(List.of(new ClienteResponse()));

        mvc.perform(post("/clientes/consultar")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nombre\":\"Juan\",\"activo\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.codigo").value(200))
                .andExpect(jsonPath("$.message").value("Consulta realizada correctamente"))
                .andExpect(jsonPath("$.data.length()").value(1));

        verify(clienteService).consultarClientes(any());
    }

    @Test
    void actualizarCliente_respondeOkConRespuestaExitosa() throws Exception {
        when(clienteService.actualizarCliente(any())).thenReturn(new ClienteResponse());
        Map<String, Object> cliente = cuerpoCliente();
        cliente.remove("fechaNacimiento");
        cliente.remove("curp");
        cliente.remove("rfc");
        cliente.put("id", 1);
        cliente.put("activo", false);

        mvc.perform(put("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cliente)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.codigo").value(200))
                .andExpect(jsonPath("$.message").value("Cliente actualizado correctamente"))
                .andExpect(jsonPath("$.data").exists());

        verify(clienteService).actualizarCliente(any());
    }

    @Test
    void solicitudInvalida_devuelveUnErrorDeCampoConLaRutaCompleta() throws Exception {
        Map<String, Object> cliente = cuerpoCliente();
        @SuppressWarnings("unchecked")
        Map<String, Object> domicilio = (Map<String, Object>) cliente.get("domicilio");
        domicilio.put("codigoPostal", "0600");

        mvc.perform(post("/clientes")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(cliente)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.codigo").value(400))
                .andExpect(jsonPath("$.errors.length()").value(1))
                .andExpect(jsonPath("$.errors[0].field").value("domicilio.codigoPostal"))
                .andExpect(jsonPath("$.errors[0].message",
                        containsString("exactamente 5 dígitos")));
    }
}
