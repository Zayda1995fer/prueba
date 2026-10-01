package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "datos_biometricos_login")
@Getter
@Setter
public class DatosBiometricosLogin {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "login_id", nullable = false, unique = true)
    private Integer loginId;

    @Column(name = "dato_biometrico_cifrado")
    private String datoBiometricoCifrado;

    @Column(name = "token_sesion_cifrado")
    private String tokenSesionCifrado;

    @Column(name = "token_sesion_hash")
    private String tokenSesionHash;

    @Column(name = "ultima_actividad")
    private LocalDateTime ultimaActividad;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }
}