package com.proyecto.servicios.config;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.deser.std.StdScalarDeserializer;

import java.io.IOException;

public class CadenaLimpiaDeserializer extends StdScalarDeserializer<String> {

    public CadenaLimpiaDeserializer() {
        super(String.class);
    }

    @Override
    public String deserialize(JsonParser parser, DeserializationContext contexto) throws IOException {
        if (parser.hasToken(JsonToken.VALUE_STRING)) {
            String limpio = parser.getText().strip();
            return limpio.isEmpty() ? null : limpio;
        }
        return (String) contexto.handleUnexpectedToken(String.class, parser);
    }
}