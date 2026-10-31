package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.SesionInterceptor;
import com.proyecto.servicios.model.onboarding.CuentaConsultaRequest;
import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.service.CuentaService;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

/**
 * Todas las rutas exigen sesión (SesionInterceptor) y solo muestran cuentas del cliente autenticado.
 * El número de cuenta viaja en el body: sin @PathVariable ni @RequestParam.
 */
@RestController
@RequestMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE)
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<CuentaResponse> consultarPorNumeroCuenta(
            @Valid @RequestBody CuentaConsultaRequest request,
            @RequestAttribute(SesionInterceptor.CLIENTE_AUTENTICADO) Integer clienteAutenticado) {
        return ResponseEntity.ok(cuentaService.consultarPorNumeroCuenta(request.getNumeroCuenta(), clienteAutenticado));
    }

    // "Consultar cuentas activas" (del cliente autenticado)
    @GetMapping("/activas")
    public ResponseEntity<List<CuentaResponse>> consultarActivas(
            @RequestAttribute(SesionInterceptor.CLIENTE_AUTENTICADO) Integer clienteAutenticado) {
        return ResponseEntity.ok(cuentaService.consultarActivas(clienteAutenticado));
    }

    // "Consultar saldo de una cuenta"
    @PostMapping(value = "/saldo", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<BigDecimal> consultarSaldo(
            @Valid @RequestBody CuentaConsultaRequest request,
            @RequestAttribute(SesionInterceptor.CLIENTE_AUTENTICADO) Integer clienteAutenticado) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(request.getNumeroCuenta(), clienteAutenticado));
    }
}
