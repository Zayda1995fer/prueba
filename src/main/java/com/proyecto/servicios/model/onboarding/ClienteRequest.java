package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Datos que se capturan al dar de alta un cliente.
 * Todas las validaciones de "forma" (formato, longitud, obligatoriedad)
 */
@Getter
@Setter
@NoArgsConstructor
public class ClienteRequest {

    // --- Datos personales ---

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{3,40}$",
            message = "El nombre solo debe contener letras y espacios, entre 3 y 40 caracteres")
    private String nombre;

    // Opcional
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

    // "No puede ser una fecha futura" -> @Past.
    // La mayoría de edad (18 años) se valida aparte en el servicio,
    // porque depende de la fecha actual, no solo de que sea pasada.
    @NotNull(message = "La fecha de nacimiento es obligatoria")
    @Past(message = "La fecha de nacimiento no puede ser una fecha futura")
    private LocalDate fechaNacimiento;

    // CURP: 4 letras + 6 dígitos (fecha) + 1 letra (H/M) + 5 consonantes + 2 caracteres
    // = 18 caracteres exactos, validados en un solo patrón.
    @NotBlank(message = "La CURP es obligatoria")
    @Pattern(regexp = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[A-Z0-9]{2}$",
            message = "La CURP debe tener el formato oficial de 18 caracteres")
    private String curp;

    // RFC persona física: 4 letras + 6 dígitos + 3 caracteres de homoclave = 13
    // (se admite también el formato corto de 12 por si se captura sin la primera letra doble)
    @NotBlank(message = "El RFC es obligatorio")
    @Pattern(regexp = "^[A-ZÑ&]{3,4}[0-9]{6}[A-Z0-9]{2,3}$",
            message = "El RFC debe tener un formato válido de 12 o 13 caracteres")
    private String rfc;

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

    // Opcional: si se captura, también debe tener 10 dígitos
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