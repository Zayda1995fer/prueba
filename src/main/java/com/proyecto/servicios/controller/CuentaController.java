package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.CuentaResponse;
import com.proyecto.servicios.service.CuentaService;
import jakarta.validation.constraints.Pattern;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.util.List;

import static com.proyecto.servicios.validation.ReglasValidacion.NUMERO_CUENTA;

@RestController
@RequestMapping(value = "/cuentas", produces = MediaType.APPLICATION_JSON_VALUE)
public class CuentaController {

    private final CuentaService cuentaService;

    public CuentaController(CuentaService cuentaService) {
        this.cuentaService = cuentaService;
    }

    // Endpoint mínimo solicitado
    @GetMapping("/{numeroCuenta}")
    public ResponseEntity<CuentaResponse> consultarPorNumeroCuenta(
            @PathVariable @Pattern(regexp = NUMERO_CUENTA,
                    message = "El número de cuenta debe tener exactamente 10 dígitos") String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarPorNumeroCuenta(numeroCuenta));
    }

    // "Consultar cuentas activas"
    @GetMapping("/activas")
    public ResponseEntity<List<CuentaResponse>> consultarActivas() {
        return ResponseEntity.ok(cuentaService.consultarActivas());
    }

    // "Consultar saldo de una cuenta"
    @GetMapping("/{numeroCuenta}/saldo")
    public ResponseEntity<BigDecimal> consultarSaldo(
            @PathVariable @Pattern(regexp = NUMERO_CUENTA,
                    message = "El número de cuenta debe tener exactamente 10 dígitos") String numeroCuenta) {
        return ResponseEntity.ok(cuentaService.consultarSaldo(numeroCuenta));
    }
}