package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.NacionalidadResponse;

import java.util.List;

public interface NacionalidadService {

    List<NacionalidadResponse> consultarTodas();
}