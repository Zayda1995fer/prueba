package com.proyecto.servicios.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Agrega el botón "Authorize" de Swagger para enviar el token de sesión a /pagos y /cuentas. */
@Configuration
public class OpenApi {

    private static final String SESION = "tokenSesion";

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .components(new Components().addSecuritySchemes(SESION, new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP).scheme("bearer")
                        .description("Token que entrega POST /login")))
                .addSecurityItem(new SecurityRequirement().addList(SESION));
    }
}
