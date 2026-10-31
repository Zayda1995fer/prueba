package com.proyecto.servicios.model.onboarding;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

import static com.proyecto.servicios.validation.ReglasValidacion.NUMERO_CUENTA;

/** Cuenta a consultar; viaja en el body (no en la URL) para no dejar el número en logs de acceso. */
@Getter
@Setter
public class CuentaConsultaRequest {

    @NotNull(message = "El número de cuenta es obligatorio")
    @Pattern(regexp = NUMERO_CUENTA, message = "El número de cuenta debe tener exactamente 10 dígitos")
    private String numeroCuenta;
}
