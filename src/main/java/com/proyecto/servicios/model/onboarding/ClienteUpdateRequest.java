package com.proyecto.servicios.model.onboarding;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
public class ClienteUpdateRequest {

    @NotBlank(message = "El nombre es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{2,50}$",
            message = "El nombre solo debe contener letras y espacios, entre 2 y 50 caracteres")
    private String nombre;

    @Pattern(regexp = "^$|^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{2,50}$",
            message = "El segundo nombre solo debe contener letras y espacios, entre 2 y 50 caracteres")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{2,50}$",
            message = "El apellido paterno solo debe contener letras y espacios, entre 2 y 50 caracteres")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ ]{2,50}$",
            message = "El apellido materno solo debe contener letras y espacios, entre 2 y 50 caracteres")
    private String apellidoMaterno;

    @NotBlank(message = "El sexo es obligatorio")
    private String sexo;

    @NotBlank(message = "La nacionalidad es obligatoria")
    private String nacionalidad;

    @NotBlank(message = "El estado civil es obligatorio")
    private String estadoCivil;

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Email(message = "El correo electrónico no tiene un formato válido")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    private String correoElectronico;

    @NotBlank(message = "El teléfono móvil es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El teléfono móvil debe contener exactamente 10 dígitos")
    private String telefonoMovil;

    @Pattern(regexp = "^$|^[0-9]{10}$", message = "El teléfono alternativo debe contener exactamente 10 dígitos")
    private String telefonoAlternativo;

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioRequest domicilio;

    @NotBlank(message = "La ocupación es obligatoria")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    private BigDecimal ingresoMensual;
}