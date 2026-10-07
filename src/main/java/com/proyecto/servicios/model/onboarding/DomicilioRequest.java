package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import static com.proyecto.servicios.validation.ReglasValidacion.*;

@Getter
@Setter
@NoArgsConstructor
public class DomicilioRequest {

    @NotBlank(message = "La calle es obligatoria")
    @Size(min = 3, max = 100, message = "La calle debe tener entre 3 y 100 caracteres")
    @Pattern(regexp = CALLE,
            message = "La calle solo puede contener letras, números, espacios y # . , ' -")
    private String calle;

    // Admite dígitos, letras (p. ej. "45-B") y "S/N" (sin número)
    @NotBlank(message = "El número exterior es obligatorio")
    @Size(max = 10, message = "El número exterior no debe exceder 10 caracteres")
    @Pattern(regexp = NUMERO_DOMICILIO,
            message = "El número exterior solo puede contener letras, números y # / - (ejemplos: 123, 45-B, S/N)")
    private String numeroExterior;

    // Opcional: no lleva @NotBlank
    @Size(max = 10, message = "El número interior no debe exceder 10 caracteres")
    @Pattern(regexp = NUMERO_DOMICILIO,
            message = "El número interior solo puede contener letras, números y # / - (ejemplos: 4, 2-A)")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Size(min = 2, max = 80, message = "La colonia debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = COLONIA,
            message = "La colonia solo puede contener letras, números, espacios y . , ' -")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Size(min = 2, max = 80, message = "El municipio debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = SOLO_LETRAS_Y_SIGNOS,
            message = "El municipio solo debe contener letras y espacios (sin números ni símbolos)")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Size(min = 2, max = 80, message = "El estado debe tener entre 2 y 80 caracteres")
    @Pattern(regexp = SOLO_LETRAS_Y_SIGNOS,
            message = "El estado solo debe contener letras y espacios (sin números ni símbolos)")
    private String estado;

    // "Debe contener exactamente 5 dígitos" (se conserva como texto para no perder ceros iniciales)
    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = CODIGO_POSTAL, message = "El código postal debe contener exactamente 5 dígitos")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    @Size(min = 2, max = 60, message = "El país debe tener entre 2 y 60 caracteres")
    @Pattern(regexp = SOLO_LETRAS_Y_SIGNOS,
            message = "El país solo debe contener letras y espacios (sin números ni símbolos)")
    private String pais;
}