package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.LoginEstatusRequest;
import com.proyecto.servicios.model.onboarding.LoginRegistroRequest;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.model.onboarding.SesionRequest;
import com.proyecto.servicios.service.LoginService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/** Todos los datos llegan en el body como Request DTO: sin @PathVariable ni @RequestParam. */
@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    // Da de alta el acceso (usuario/contraseña) de un cliente ya registrado
    @PostMapping(value = "/clientes/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> registrarAcceso(@Valid @RequestBody LoginRegistroRequest request) {
        loginService.registrarAcceso(request.getIdCliente(), request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Activa o desactiva el acceso del cliente
    @PutMapping(value = "/clientes/login/estatus", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> cambiarEstatus(@Valid @RequestBody LoginEstatusRequest request) {
        loginService.cambiarEstatusAcceso(request.getIdCliente(), request.getActivo());
        return ResponseEntity.noContent().build();
    }

    // Autenticación: entrega un token de sesión válido por N minutos (5 por defecto)
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> autenticar(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.autenticar(request));
    }

    // Valida si el token de sesión sigue vigente (dentro del límite de 5 minutos)
    @PostMapping(value = "/login/validar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Integer> validarSesion(@Valid @RequestBody SesionRequest request) {
        return ResponseEntity.ok(loginService.validarSesion(request.getTokenSesion()));
    }
}
