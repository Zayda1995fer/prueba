package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.NacionalidadResponse;
import com.proyecto.servicios.service.NacionalidadService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Catálogo de nacionalidades permitidas (usado al capturar un cliente en
 * /clientes, campo nacionalidadId). Solo lectura: el catálogo se carga
 * una sola vez desde la migración V3__create_catalogo_nacionalidades.sql.
 */
@RestController
@RequestMapping(value = "/nacionalidades", produces = MediaType.APPLICATION_JSON_VALUE)
public class NacionalidadController {

    private final NacionalidadService nacionalidadService;

    public NacionalidadController(NacionalidadService nacionalidadService) {
        this.nacionalidadService = nacionalidadService;
    }

    @GetMapping
    public ResponseEntity<List<NacionalidadResponse>> consultarTodas() {
        return ResponseEntity.ok(nacionalidadService.consultarTodas());
    }
}