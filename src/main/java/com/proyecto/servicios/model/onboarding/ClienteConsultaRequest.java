package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;
import java.util.Locale;

import static com.proyecto.servicios.validation.ReglasValidacion.*;

@Getter
@Setter
@NoArgsConstructor
public class ClienteConsultaRequest {

    @Positive(message = "El id debe ser un entero positivo")
    private Integer id;

    @Size(min = 2, max = 40, message = "El nombre debe tener entre 2 y 40 caracteres")
    @Pattern(regexp = NOMBRE_PERSONA,
             message = "El nombre solo debe contener letras (con acentos o ñ) separadas por un espacio")
    private String nombre;

    @Size(min = 2, max = 40, message = "El apellido paterno debe tener entre 2 y 40 caracteres")
    @Pattern(regexp = NOMBRE_PERSONA,
             message = "El apellido paterno solo debe contener letras (con acentos o ñ) separadas por un espacio")
    private String apellidoPaterno;

    @Size(min = 2, max = 40, message = "El apellido materno debe tener entre 2 y 40 caracteres")
    @Pattern(regexp = NOMBRE_PERSONA,
             message = "El apellido materno solo debe contener letras (con acentos o ñ) separadas por un espacio")
    private String apellidoMaterno;

    @Pattern(regexp = CURP, message = "La CURP debe tener 18 caracteres con el formato oficial (solo letras y números)")
    private String curp;

    @Pattern(regexp = RFC_PERSONA_FISICA,
             message = "El RFC de persona física debe tener 13 caracteres con el formato oficial (4 letras, 6 dígitos de fecha y 3 de homoclave)")
    private String rfc;

    @Size(max = 100, message = "El correo electrónico no debe exceder 100 caracteres")
    @Pattern(regexp = CORREO,
             message = "El correo electrónico no tiene un formato válido (ejemplo: usuario@dominio.com, sin espacios)")
    private String correoElectronico;

    @Pattern(regexp = "^[0-9]{10}$", message = "El número de cuenta debe tener exactamente 10 dígitos")
    private String numeroCuenta;

    private Boolean activo;

    private LocalDate fechaCreacionDesde;

    private LocalDate fechaCreacionHasta;

    public void setCurp(String curp) {
        this.curp = curp == null ? null : curp.toUpperCase(Locale.ROOT);
    }

    public void setRfc(String rfc) {
        this.rfc = rfc == null ? null : rfc.toUpperCase(Locale.ROOT);
    }

    public void setCorreoElectronico(String correoElectronico) {
        this.correoElectronico = correoElectronico == null ? null : correoElectronico.toLowerCase(Locale.ROOT);
    }
}