package com.proyecto.servicios.controller;

import org.junit.jupiter.api.Test;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Comprueba las restricciones del proyecto sobre ClienteController:
 * ningún endpoint usa @PathVariable ni @RequestParam, y todo parámetro
 * de entrada es un DTO recibido con @RequestBody.
 */
class ClienteControllerRestriccionesTest {

    private static List<Method> endpoints() {
        return Arrays.stream(ClienteController.class.getDeclaredMethods())
                .filter(m -> Modifier.isPublic(m.getModifiers()))
                .toList();
    }

    @Test
    void hayEndpointsDefinidos() {
        assertThat(endpoints()).hasSize(3); // registrar, consultar, actualizar
    }

    @Test
    void ningunEndpointUsaPathVariable() {
        for (Method m : endpoints()) {
            for (Parameter p : m.getParameters()) {
                assertThat(p.isAnnotationPresent(PathVariable.class))
                        .as("%s no debe usar @PathVariable", m.getName()).isFalse();
            }
        }
    }

    @Test
    void ningunEndpointUsaRequestParam() {
        for (Method m : endpoints()) {
            for (Parameter p : m.getParameters()) {
                assertThat(p.isAnnotationPresent(RequestParam.class))
                        .as("%s no debe usar @RequestParam", m.getName()).isFalse();
            }
        }
    }

    @Test
    void todoParametroDeEntradaSeRecibeConRequestBody() {
        for (Method m : endpoints()) {
            for (Parameter p : m.getParameters()) {
                assertThat(p.isAnnotationPresent(RequestBody.class))
                        .as("%s: el parámetro %s debe venir en @RequestBody", m.getName(), p.getName()).isTrue();
            }
        }
    }
}
