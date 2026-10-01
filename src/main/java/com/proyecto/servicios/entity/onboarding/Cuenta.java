package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "cuentas")
@Getter
@Setter
public class Cuenta {

    public static final String ESTATUS_ACTIVA = "ACTIVA";
    public static final String ESTATUS_INACTIVA = "INACTIVA";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cliente_id", nullable = false)
    private Integer clienteId;

    @Column(name = "numero_cuenta", nullable = false, unique = true)
    private String numeroCuenta;

    @Column(name = "saldo", nullable = false)
    private BigDecimal saldo;

    @Column(name = "estatus", nullable = false)
    private String estatus;

    @Column(name = "fecha_apertura", nullable = false, updatable = false)
    private LocalDateTime fechaApertura;

    @PrePersist
    void alCrear() {
        fechaApertura = LocalDateTime.now();
    }
}
