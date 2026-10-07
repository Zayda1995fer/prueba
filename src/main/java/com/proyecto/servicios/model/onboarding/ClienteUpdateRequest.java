package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.validation.TelefonoMexicano;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.Locale;

import static com.proyecto.servicios.validation.ReglasValidacion.*;

@Getter
@Setter
@NoArgsConstructor
public class ClienteUpdateRequest {

    // --- Datos personales (excepto CURP/RFC) ---

    @NotBlank(message = "El nombre es obligatorio")
    @Size(min = 2, max = 40, message = "El nombre debe tener entre 2 y 40 caracteres")
    @Pattern(regexp = NOMBRE_PERSONA,
            message = "El nombre solo debe contener letras (con acentos o ñ) separadas por un espacio")
    private String nombre;

    @Size(min = 2, max = 40, message = "El segundo nombre debe tener entre 2 y 40 caracteres")
    @Pattern(regexp = NOMBRE_PERSONA,
            message = "El segundo nombre solo debe contener letras (con acentos o ñ) separadas por un espacio")
    private String segundoNombre;

    @NotBlank(message = "El apellido paterno es obligatorio")
    @Size(min = 2, max = 40, message = "El apellido paterno debe tener entre 2 y 40 caracteres")
    @Pattern(regexp = NOMBRE_PERSONA,
            message = "El apellido paterno solo debe contener letras (con acentos o ñ) separadas por un espacio")
    private String apellidoPaterno;

    @NotBlank(message = "El apellido materno es obligatorio")
    @Size(min = 2, max = 40, message = "El apellido materno debe tener entre 2 y 40 caracteres")
    @Pattern(regexp = NOMBRE_PERSONA,
            message = "El apellido materno solo debe contener letras (con acentos o ñ) separadas por un espacio")
    private String apellidoMaterno;

    @NotBlank(message = "El sexo es obligatorio")
    @Pattern(regexp = SEXO, message = "El sexo debe capturarse como \"H\" o \"M\"")
    private String sexo;

    // Debe ser el id de una nacionalidad del catálogo (GET /nacionalidades)
    @NotNull(message = "La nacionalidad es obligatoria")
    @Positive(message = "La nacionalidad debe ser un id positivo del catálogo")
    private Integer nacionalidadId;

    @NotBlank(message = "El estado civil es obligatorio")
    @Pattern(regexp = ESTADO_CIVIL,
            message = "El estado civil debe ser uno de: Soltero(a), Casado(a), Divorciado(a), Viudo(a), Unión libre")
    private String estadoCivil;

    // --- Datos de contacto ---

    @NotBlank(message = "El correo electrónico es obligatorio")
    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    @Pattern(regexp = CORREO,
            message = "El correo electrónico no tiene un formato válido (ejemplo: usuario@dominio.com, sin espacios)")
    private String correoElectronico;

    @NotNull(message = "El teléfono móvil es obligatorio")
    @TelefonoMexicano(campo = "El teléfono móvil")
    private Long telefonoMovil;

    @TelefonoMexicano(campo = "El teléfono alternativo")
    private Long telefonoAlternativo;

    // --- Domicilio ---

    @NotNull(message = "El domicilio es obligatorio")
    @Valid
    private DomicilioRequest domicilio;

    // --- Información laboral ---

    @NotBlank(message = "La ocupación es obligatoria")
    @Size(min = 3, max = 60, message = "La ocupación debe tener entre 3 y 60 caracteres")
    @Pattern(regexp = OCUPACION, message = "La ocupación solo debe contener letras y espacios")
    private String ocupacion;

    @NotBlank(message = "La empresa es obligatoria")
    @Size(min = 2, max = 80, message = "La empresa debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = EMPRESA,
            message = "La empresa solo puede contener letras, números, espacios y . , & ' -")
    private String empresa;

    @NotNull(message = "El ingreso mensual es obligatorio")
    @DecimalMin(value = "0.01", message = "El ingreso mensual debe ser mayor a cero")
    @Digits(integer = 7, fraction = 2,
            message = "El ingreso mensual admite máximo 7 dígitos enteros y 2 decimales (máximo 9,999,999.99)")
    private BigDecimal ingresoMensual;

    // --- Normalización previa a la validación ---

    public void setSexo(String sexo) {
        this.sexo = sexo == null ? null : sexo.toUpperCase(Locale.ROOT);
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico == null ? null : correoElectronico.toLowerCase(Locale.ROOT);
    }
}