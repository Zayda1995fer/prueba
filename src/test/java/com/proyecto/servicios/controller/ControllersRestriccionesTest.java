package com.proyecto.servicios.controller;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Parameter;

import static org.assertj.core.api.Assertions.assertThat;

/** Ningún controller del proyecto recibe datos por @PathVariable ni @RequestParam. */
class ControllersRestriccionesTest {

    @ParameterizedTest
    @ValueSource(classes = {ClienteController.class, CuentaController.class, LoginController.class,
            PagoController.class, NacionalidadController.class, PersonaController.class, ProductController.class})
    void sinPathVariableNiRequestParam(Class<?> controller) {
        for (Method m : controller.getDeclaredMethods()) {
            if (!Modifier.isPublic(m.getModifiers())) {
                continue;
            }
            for (Parameter p : m.getParameters()) {
                assertThat(p.isAnnotationPresent(PathVariable.class) || p.isAnnotationPresent(RequestParam.class))
                        .as("%s.%s no debe usar @PathVariable ni @RequestParam", controller.getSimpleName(), m.getName())
                        .isFalse();
            }
        }
    }
}
