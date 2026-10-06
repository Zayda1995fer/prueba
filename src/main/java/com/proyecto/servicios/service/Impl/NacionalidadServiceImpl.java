package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Nacionalidad;
import com.proyecto.servicios.model.onboarding.NacionalidadResponse;
import com.proyecto.servicios.repositorys.onboarding.NacionalidadRepository;
import com.proyecto.servicios.service.NacionalidadService;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;

@Service
public class NacionalidadServiceImpl implements NacionalidadService {

    private final NacionalidadRepository nacionalidadRepository;

    public NacionalidadServiceImpl(NacionalidadRepository nacionalidadRepository) {
        this.nacionalidadRepository = nacionalidadRepository;
    }

    @Override
    public List<NacionalidadResponse> consultarTodas() {
        return nacionalidadRepository.findAll().stream()
                .sorted(Comparator.comparing(Nacionalidad::getNombre))
                .map(NacionalidadServiceImpl::mapear)
                .toList();
    }

    public static NacionalidadResponse mapear(Nacionalidad nacionalidad) {
        NacionalidadResponse response = new NacionalidadResponse();
        response.setId(nacionalidad.getId());
        response.setNombre(nacionalidad.getNombre());
        return response;
    }
}