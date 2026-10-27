package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.PagoConsultaRequest;
import com.proyecto.servicios.model.onboarding.PagoRequest;
import com.proyecto.servicios.model.onboarding.PagoResponse;

public interface PagoService {

    /** Realiza el pago completo. EXITOSO si el proveedor lo confirmó; POR_CONCILIAR si aún no se sabe. */
    PagoResponse pagar(PagoRequest request);

    /** Estado actual de un pago del cliente, por su id de solicitud. */
    PagoResponse consultarPago(PagoConsultaRequest request);
}
