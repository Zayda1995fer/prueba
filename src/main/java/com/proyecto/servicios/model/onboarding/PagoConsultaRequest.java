package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

/** Consulta del estado de un pago por la misma llave de idempotencia con la que se creó. */
@Getter
@Setter
public class PagoConsultaRequest {

    @NotNull(message = "El id de la solicitud es obligatorio")
    @Pattern(regexp = "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$",
            message = "El id de la solicitud debe ser un UUID")
    private String idSolicitud;

    @NotNull(message = "El id del cliente es obligatorio")
    @Positive(message = "El id del cliente debe ser un número positivo")
    private Integer idUser;
}
