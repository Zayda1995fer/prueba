package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Datos permitidos para actualizar un cliente.
 *
 * A propósito NO incluye curp, rfc ni numeroCuenta: la regla de negocio
 * dice que esos tres datos no se pueden modificar una vez creados. Al no
 * existir el campo en este DTO, es físicamente imposible que alguien lo
 * mande a actualizar por esta vía (no hace falta "ignorarlo" a mano en
 * el servicio, el propio contrato del API ya lo impide).
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteUpdateRequest {

    // --- Datos personales (excepto CURP/RFC) ---

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
    @Pattern(regexp = "^[HM]$", message = "El sexo debe capturarse como \"H\" o \"M\"")
    private String sexo;

    // Ya no es texto libre: debe ser el id de una nacionalidad del
    // catálogo (GET /nacionalidades).
    @NotNull(message = "La nacionalidad es obligatoria")
    private Integer nacionalidadId;

    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(regexp = "^(Soltero\\(a\\)|Casado\\(a\\)|Divorciado\\(a\\)|Viudo\\(a\\)|Unión libre)$",
            message = "El estado civil debe ser uno de: Soltero(a), Casado(a), Divorciado(a), Viudo(a), Unión libre")
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
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{3,60}$",
            message = "La ocupación solo debe contener letras y espacios, entre 3 y 60 caracteres")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ0-9 .,&'-]{2,80}$",
            message = "La empresa solo puede contener letras, números, espacios y . , & ' -, entre 2 y 80 caracteres")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 10, fraction = 2, message = "El ingreso mensual admite máximo 2 decimales")
    private BigDecimal ingresoMensual;
}