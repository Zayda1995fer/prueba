package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.LoginRegistroRequest;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;

public interface LoginService {

    void registrarAcceso(Integer clienteId, LoginRegistroRequest request);

    LoginResponse autenticar(LoginRequest request);

    Integer validarSesion(String tokenSesion);

    void cambiarEstatusAcceso(Integer clienteId, boolean activo);
}