package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.LoginRegistroRequest;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    @PostMapping(value = "/clientes/{id}/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> registrarAcceso(@PathVariable Integer id,
                                                @Valid @RequestBody LoginRegistroRequest request) {
        loginService.registrarAcceso(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PutMapping("/clientes/{id}/login/estatus")
    public ResponseEntity<Void> cambiarEstatus(@PathVariable Integer id, @RequestParam boolean activo) {
        loginService.cambiarEstatusAcceso(id, activo);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> autenticar(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.autenticar(request));
    }

    @GetMapping("/login/validar/{token}")
    public ResponseEntity<Integer> validarSesion(@PathVariable String token) {
        return ResponseEntity.ok(loginService.validarSesion(token));
    }
}