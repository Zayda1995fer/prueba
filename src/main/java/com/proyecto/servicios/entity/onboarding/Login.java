package com.proyecto.servicios.entity.onboarding;


import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(name = "login")
@Getter
@Setter
public class Login {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cliente_id", nullable = false, unique = true)
    private Integer clienteId;

    @Column(name = "usuario", nullable = false, unique = true)
    private String usuario;

    @Column(name = "contrasena", nullable = false)
    private String contrasena;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }
}