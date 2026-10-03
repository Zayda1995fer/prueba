package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Datos permitidos para actualizar un cliente.
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteUpdateRequest {

    // --- Datos personales ---

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{3,40}$",
            message = "El nombre solo debe contener letras y espacios, entre 3 y 40 caracteres")
    private String nombre;

    @Pattern(regexp = "^$|^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{3,40}$",
            message = "El segundo nombre solo debe contener letras y espacios, entre 3 y 40 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{3,40}$",
            message = "El apellido paterno solo debe contener letras y espacios, entre 3 y 40 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{3,40}$",
            message = "El apellido materno solo debe contener letras y espacios, entre 3 y 40 caracteres")
    private String apellidoMaterno;

    @NotBlank(message = "El sexo es obligatorio")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    private String estadoCivil;

    // --- Datos de contacto ---

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    private String correoElectronico;

    @NotNull(message = "El teléfono móvil es obligatorio")
    @Digits(integer = 10, fraction = 0, message = "El teléfono móvil debe contener exactamente 10 dígitos")
    @Min(value = 1_000_000_000L, message = "El teléfono móvil debe contener exactamente 10 dígitos")
    @Max(value = 9_999_999_999L, message = "El teléfono móvil debe contener exactamente 10 dígitos")
    private Long telefonoMovil;

    @Digits(integer = 10, fraction = 0, message = "El teléfono alternativo debe contener exactamente 10 dígitos")
    @Min(value = 1_000_000_000L, message = "El teléfono alternativo debe contener exactamente 10 dígitos")
    @Max(value = 9_999_999_999L, message = "El teléfono alternativo debe contener exactamente 10 dígitos")
    private Long telefonoAlternativo;

    // --- Domicilio ---

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioRequest domicilio;

    // --- Información laboral ---

    @NotBlank(message = "La ocupación es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;
}