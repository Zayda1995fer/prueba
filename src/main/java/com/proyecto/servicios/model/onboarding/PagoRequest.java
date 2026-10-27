package com.proyecto.servicios.model.onboarding;

import com.proyecto.servicios.validation.TelefonoMexicano;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Pago de un servicio. El producto del catálogo decide si se pide teléfono
 * o referencia: se manda uno solo, nunca los dos.
 */
@Getter
@Setter
public class PagoRequest {

    @NotNull(message = "El id de la solicitud es obligatorio")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "El id de la solicitud debe ser un UUID (por ejemplo 3f2b8c1e-9a4d-4c7e-8b21-5d6e7f8a9b0c)")
    private String idSolicitud;

    @NotNull(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = "^[0-9]{10}$", message = "El número de cuenta debe tener exactamente 10 dígitos")
    private String numeroCuenta;

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un número positivo")
    private Integer idUser;

    @NotNull(message = "El id del servicio es obligatorio")
    @Positive(message = "El id del servicio debe ser un número positivo")
    private Integer idServicio;

    @NotNull(message = "El id del producto es obligatorio")
    @Positive(message = "El id del producto debe ser un número positivo")
    private Integer idProducto;

    @NotNull(message = "El monto es obligatorio")
    @DecimalMin(value = "0.01", message = "El monto debe ser mayor a cero")
    @Digits(integer = 7, fraction = 2, message = "El monto admite máximo 7 enteros y 2 decimales")
    private BigDecimal monto;

    @TelefonoMexicano(campo = "El teléfono")
    private Long telefono;

    @Pattern(regexp = "^[A-Za-z0-9]{5,30}$",
            message = "La referencia debe tener entre 5 y 30 caracteres, solo letras y números")
    private String referencia;

    @NotNull(message = "El código de verificación (2FA) es obligatorio")
    @Pattern(regexp = "^[0-9]{6}$", message = "El código de verificación debe tener 6 dígitos")
    private String codigo2fa;
}
