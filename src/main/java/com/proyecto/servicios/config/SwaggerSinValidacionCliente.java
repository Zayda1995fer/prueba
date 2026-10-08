package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.PathItem;
import io.swagger.v3.oas.models.parameters.Parameter;
import org.springdoc.core.customizers.OpenApiCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SwaggerSinValidacionCliente {

    @Bean
    public OpenApiCustomizer permitirEnviarSinValidarEnSwagger() {
        return openApi -> {
            if (openApi.getPaths() == null) {
                return;
            }
            for (PathItem item : openApi.getPaths().values()) {
                for (Operation operacion : item.readOperations()) {
                    if (operacion.getParameters() != null) {
                        for (Parameter p : operacion.getParameters()) {
                            if (!"path".equals(p.getIn())) {
                                p.setRequired(false);
                            }
                        }
                    }
                    if (operacion.getRequestBody() != null) {
                        operacion.getRequestBody().setRequired(false);
                    }
                }
            }
        };
    }
}