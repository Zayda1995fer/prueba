package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;

import java.time.LocalDateTime;
import java.util.List;

public interface ClienteService {

    ClienteResponse registrarCliente(ClienteRequest request);

    List<ClienteResponse> consultarTodos();

    ClienteResponse consultarPorId(Integer id);

    ClienteResponse consultarPorCurp(String curp);

    ClienteResponse consultarPorRfc(String rfc);

    ClienteResponse consultarPorCorreo(String correo);

    ClienteResponse consultarPorNumeroCuenta(String numeroCuenta);

    List<ClienteResponse> consultarActivos();

    List<ClienteResponse> consultarPorRangoDeFechas(LocalDateTime desde, LocalDateTime hasta);

    ClienteResponse actualizarCliente(Integer id, ClienteUpdateRequest request);

    void darDeBajaCliente(Integer id);
}