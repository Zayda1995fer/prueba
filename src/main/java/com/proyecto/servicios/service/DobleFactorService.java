package com.proyecto.servicios.service;

import com.proyecto.servicios.model.onboarding.Codigo2faRequest;
import com.proyecto.servicios.model.onboarding.Codigo2faResponse;

/** Segundo factor propio (código de un solo uso, como el de las apps de banco) para autorizar pagos. */
public interface DobleFactorService {

    /** Genera un código nuevo e invalida los anteriores de esa cuenta. */
    Codigo2faResponse solicitar(Codigo2faRequest request, Integer clienteAutenticado);

    /** Valida y consume el código; lanza NegocioException si no es válido. */
    void validar(Integer clienteId, Integer cuentaId, String codigo);
}
