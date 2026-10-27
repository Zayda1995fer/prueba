package com.proyecto.servicios.exception;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Header exception: una cabecera obligatoria ausente responde 400 con el formato estándar. */
class CabeceraObligatoriaTest {

    @RestController
    static class ControllerConCabecera {
        @GetMapping("/prueba-cabecera")
        public String leer(@RequestHeader("X-Canal") String canal) {
            return canal;
        }
    }

    private final MockMvc mvc = MockMvcBuilders.standaloneSetup(new ControllerConCabecera())
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();

    @Test
    void sinLaCabecera_respondeBadRequestIndicandoCualFalta() throws Exception {
        mvc.perform(get("/prueba-cabecera").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.field").value("X-Canal"))
                .andExpect(jsonPath("$.message").value("La cabecera 'X-Canal' es obligatoria"));
    }

    @Test
    void conLaCabecera_pasa() throws Exception {
        mvc.perform(get("/prueba-cabecera").header("X-Canal", "web")).andExpect(status().isOk());
    }
}
