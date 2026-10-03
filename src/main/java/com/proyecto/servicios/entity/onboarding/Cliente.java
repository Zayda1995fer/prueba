package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Cliente persona física.
 * Refleja la tabla "clientes" creada en V2__create_onboarding_clientes.sql.
 */
@Entity
@Table(name = "clientes")
@Getter
@Setter
public class Cliente {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "segundo_nombre")
    private String segundoNombre;

    @Column(name = "apellido_paterno", nullable = false)
    private String apellidoPaterno;

    @Column(name = "apellido_materno", nullable = false)
    private String apellidoMaterno;

    @Column(name = "fecha_nacimiento", nullable = false)
    private LocalDate fechaNacimiento;

    // CURP y RFC nunca se modifican una vez creado el cliente (regla de negocio);
    // por eso el servicio, no la entidad, es responsable de no tocarlos en un update.
    @Column(name = "curp", nullable = false, unique = true)
    private String curp;

    @Column(name = "rfc", nullable = false, unique = true)
    private String rfc;

    @Column(name = "sexo", nullable = false)
    private String sexo;

    @Column(name = "nacionalidad", nullable = false)
    private String nacionalidad;

    @Column(name = "estado_civil", nullable = false)
    private String estadoCivil;

    @Column(name = "correo_electronico", nullable = false, unique = true)
    private String correoElectronico;

    @Column(name = "telefono_movil", nullable = false)
    private Long telefonoMovil;

    @Column(name = "telefono_alternativo")
    private Long telefonoAlternativo;

    @Column(name = "ocupacion", nullable = false)
    private String ocupacion;

    @Column(name = "empresa", nullable = false)
    private String empresa;

    @Column(name = "ingreso_mensual", nullable = false)
    private BigDecimal ingresoMensual;

    // Baja lógica: nunca se borra un cliente físicamente, solo se marca activo = false
    @Column(name = "activo", nullable = false)
    private Boolean activo = true;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = LocalDateTime.now();
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }
}