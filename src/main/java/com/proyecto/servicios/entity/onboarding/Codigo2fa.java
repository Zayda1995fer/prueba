package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

/** Código de un solo uso para autorizar un pago. Solo se guarda su hash, nunca el código. */
@Entity
@Table(name = "codigos_2fa")
@Getter
@Setter
public class Codigo2fa {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(name = "cuenta_id", nullable = false)
    private Integer cuentaId;

    @Column(name = "codigo_hash", nullable = false)
    private String codigoHash;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_expiracion", nullable = false)
    private LocalDateTime fechaExpiracion;

    @Column(name = "intentos", nullable = false)
    private Integer intentos = 0;

    @Column(name = "usado", nullable = false)
    private Boolean usado = false;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
    }
}
