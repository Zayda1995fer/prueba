package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class DomicilioRequest {

    @NotBlank(message = "La calle es obligatoria")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ0-9 #.,'-]{3,100}$",
            message = "La calle solo puede contener letras, números, espacios y # . , ' -, entre 3 y 100 caracteres")
    private String calle;

    // Admite dígitos, letras (p. ej. "45-B") y "S/N" (sin número)
    @NotBlank(message = "El número exterior es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ0-9#/-]{1,10}$",
            message = "El número exterior no tiene un formato válido")
    private String numeroExterior;

    // Opcional: no lleva @NotBlank
    @Pattern(regexp = "^$|^[A-Za-zÁÉÍÓÚÑáéíóúñ0-9#/-]{1,10}$",
            message = "El número interior no tiene un formato válido")
    private String numeroInterior;

    @NotBlank(message = "La colonia es obligatoria")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ0-9 .,'-]{2,80}$",
            message = "La colonia solo puede contener letras, números, espacios y . , ' -, entre 2 y 80 caracteres")
    private String colonia;

    @NotBlank(message = "El municipio es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ .'-]{2,80}$",
            message = "El municipio solo debe contener letras y espacios, entre 2 y 80 caracteres")
    private String municipio;

    @NotBlank(message = "El estado es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ .'-]{2,80}$",
            message = "El estado solo debe contener letras y espacios, entre 2 y 80 caracteres")
    private String estado;

    // "Debe contener exactamente 5 dígitos"
    @NotBlank(message = "El código postal es obligatorio")
    @Pattern(regexp = "^[0-9]{5}$", message = "El código postal debe contener exactamente 5 dígitos")
    private String codigoPostal;

    @NotBlank(message = "El país es obligatorio")
    @Pattern(regexp = "^[A-Za-zÁÉÍÓÚÑáéíóúñ .'-]{2,60}$",
            message = "El país solo debe contener letras y espacios, entre 2 y 60 caracteres")
    private String pais;
}