package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.LoginRegistroRequest;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.service.LoginService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.proyecto.servicios.validation.ReglasValidacion.BOOLEANO_TEXTO;
import static com.proyecto.servicios.validation.ReglasValidacion.TOKEN_SESION;

@RestController
@RequestMapping(produces = MediaType.APPLICATION_JSON_VALUE)
public class LoginController {

    private final LoginService loginService;

    public LoginController(LoginService loginService) {
        this.loginService = loginService;
    }

    // Da de alta el acceso (usuario/contraseña) de un cliente ya registrado
    @PostMapping(value = "/clientes/{id}/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<Void> registrarAcceso(@PathVariable @Positive(message = "El id debe ser un entero positivo") Integer id,
                                                @Valid @RequestBody LoginRegistroRequest request) {
        loginService.registrarAcceso(id, request);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    // Activa o desactiva el acceso del cliente ("bandera" true/false)
    @PutMapping("/clientes/{id}/login/estatus")
    public ResponseEntity<Void> cambiarEstatus(
            @PathVariable @Positive(message = "El id debe ser un entero positivo") Integer id,
            @RequestParam @Pattern(regexp = BOOLEANO_TEXTO,
                    message = "El parámetro 'activo' solo acepta los valores true o false") String activo) {
        loginService.cambiarEstatusAcceso(id, Boolean.parseBoolean(activo));
        return ResponseEntity.noContent().build();
    }

    // Autenticación: entrega un token de sesión válido por N minutos (5 por defecto)
    @PostMapping(value = "/login", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<LoginResponse> autenticar(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(loginService.autenticar(request));
    }

    // Valida si el token de sesión sigue vigente (dentro del límite de 5 minutos)
    @GetMapping("/login/validar/{token}")
    public ResponseEntity<Integer> validarSesion(
            @PathVariable @Pattern(regexp = TOKEN_SESION,
                    message = "El token de sesión no tiene un formato válido") String token) {
        return ResponseEntity.ok(loginService.validarSesion(token));
    }
}