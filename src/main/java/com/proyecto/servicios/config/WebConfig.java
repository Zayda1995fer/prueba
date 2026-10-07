package com.proyecto.servicios.config;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.CoercionAction;
import com.fasterxml.jackson.databind.cfg.CoercionInputShape;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.type.LogicalType;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.List;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ObjectMapper objectMapperBase;

    public WebConfig(ObjectMapper objectMapperBase) {
        this.objectMapperBase = objectMapperBase;
    }

    @Override
    public void extendMessageConverters(List<HttpMessageConverter<?>> converters) {
        for (HttpMessageConverter<?> converter : converters) {
            if (converter instanceof MappingJackson2HttpMessageConverter json) {
                json.setObjectMapper(crearMapperEstricto(objectMapperBase));
            }
        }
    }

    /** Público y estático para que las pruebas usen exactamente la misma configuración. */
    public static ObjectMapper crearMapperEstricto(ObjectMapper base) {
        ObjectMapper mapper = base.copy();

        mapper.enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        mapper.enable(DeserializationFeature.FAIL_ON_NULL_FOR_PRIMITIVES);
        mapper.enable(DeserializationFeature.FAIL_ON_TRAILING_TOKENS);
        mapper.disable(DeserializationFeature.ACCEPT_FLOAT_AS_INT);

        // Entero, decimal y boolean no se aceptan escritos como texto
        // ("55#$%", "123", "true") ni como otro tipo (1 / 0 como boolean).
        mapper.coercionConfigFor(LogicalType.Integer).setCoercion(CoercionInputShape.String, CoercionAction.Fail);
        mapper.coercionConfigFor(LogicalType.Float).setCoercion(CoercionInputShape.String, CoercionAction.Fail);
        mapper.coercionConfigFor(LogicalType.Boolean).setCoercion(CoercionInputShape.String, CoercionAction.Fail);
        mapper.coercionConfigFor(LogicalType.Boolean).setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
        mapper.coercionConfigFor(LogicalType.Boolean).setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
        // y el texto no se acepta escrito como número / boolean
        mapper.coercionConfigFor(LogicalType.Textual).setCoercion(CoercionInputShape.Integer, CoercionAction.Fail);
        mapper.coercionConfigFor(LogicalType.Textual).setCoercion(CoercionInputShape.Float, CoercionAction.Fail);
        mapper.coercionConfigFor(LogicalType.Textual).setCoercion(CoercionInputShape.Boolean, CoercionAction.Fail);

        SimpleModule cadenas = new SimpleModule("CadenasLimpias");
        cadenas.addDeserializer(String.class, new CadenaLimpiaDeserializer());
        mapper.registerModule(cadenas);
        return mapper;
    }
}