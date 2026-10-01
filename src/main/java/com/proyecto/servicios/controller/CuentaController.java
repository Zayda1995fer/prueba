package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.service.CuentaService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE)
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponse> consultarPorNumeroCuenta(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarPorNumeroCuenta(numeroCuenta));
    }

    @GetMapping("/activas")
    public ResponseEntity<List<CuentaResponse>> consultarActivas() {
        return ResponseEntity.ok(cuentaService.consultarActivas());
    }

    @GetMapping("/{numeroCuenta}/saldo")
    public ResponseEntity<BigDecimal> consultarSaldo(@PathVariable String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(numeroCuenta));
    }
}
