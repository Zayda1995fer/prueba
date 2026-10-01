package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

@Entity
@Table(name = "domicilios")
@Getter
@Setter
public class Domicilio {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @Column(name = "cliente_id", nullable = false, unique = true)
    private Integer clienteId;

    @Column(name = "calle", nullable = false)
    private String calle;

    @Column(name = "numero_exterior", nullable = false)
    private String numeroExterior;

    @Column(name = "numero_interior")
    private String numeroInterior;

    @Column(name = "colonia", nullable = false)
    private String colonia;

    @Column(name = "municipio", nullable = false)
    private String municipio;

    @Column(name = "estado", nullable = false)
    private String estado;

    @Column(name = "codigo_postal", nullable = false)
    private String codigoPostal;

    @Column(name = "pais", nullable = false)
    private String pais;
}
