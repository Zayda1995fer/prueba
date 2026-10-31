package com.proyecto.servicios.validation;

import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;

/**
 * Autorización: el cliente de la sesión (SesionInterceptor) solo puede operar
 * a su propio nombre. El idUser del body nunca basta por sí solo.
 */
public final class VerificadorTitular {

    private VerificadorTitular() {
    }

    public static void verificar(Integer clienteAutenticado, Integer clienteSolicitado) {
        if (clienteAutenticado == null || !clienteAutenticado.equals(clienteSolicitado)) {
            throw new NegocioException(CatalogoError.OPERACION_NO_AUTORIZADA);
        }
    }
}
