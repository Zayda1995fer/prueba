package com.proyecto.servicios.model.onboarding;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LoginResponse {
    private String tokenSesion;
    private Integer duracionSesionMinutos;
}