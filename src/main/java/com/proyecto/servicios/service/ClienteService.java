package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.ClienteConsultaRequest;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;

import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(ClienteRequest request);

    // Único método de consulta: filtros opcionales y combinables (ver ClienteConsultaRequest)
    List<ClienteResponse> consultarClientes(ClienteConsultaRequest criterios);

    ClienteResponse actualizarCliente(ClienteUpdateRequest request);
}
