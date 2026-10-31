package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.service.LoginService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import static com.proyecto.servicios.validation.ReglasValidacion.TOKEN_SESION;

/**
 * Identifica al cliente ANTES de llegar al controller: exige la cabecera
 * "Authorization: Bearer <tokenSesion>" (el token que entrega /login), la valida con
 * LoginService (existe, no expiró, renueva la ventana de inactividad) y deja el id del
 * cliente autenticado como atributo de la petición.
 *
 * El id del cliente NUNCA se toma como verdadero solo porque viene en el body: los
 * servicios comparan el idUser del body contra este atributo (403 si no coinciden).
 * Las excepciones que se lanzan aquí las procesa GlobalExceptionHandler como cualquier otra.
 */
@Component
public class SesionInterceptor implements HandlerInterceptor {

    /** Nombre del atributo de la petición con el id del cliente autenticado. */
    public static final String CLIENTE_AUTENTICADO = "clienteAutenticado";

    private static final String PREFIJO = "Bearer ";

    private final LoginService loginService;

    public SesionInterceptor(LoginService loginService) {
        this.loginService = loginService;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String cabecera = request.getHeader(HttpHeaders.AUTHORIZATION);
        if (cabecera == null || !cabecera.startsWith(PREFIJO)) {
            throw new NegocioException(CatalogoError.SESION_REQUERIDA);
        }
        String token = cabecera.substring(PREFIJO.length()).trim();
        // Formato inválido: se rechaza sin ir a la base de datos
        if (!token.matches(TOKEN_SESION)) {
            throw new NegocioException(CatalogoError.TOKEN_INVALIDO);
        }
        request.setAttribute(CLIENTE_AUTENTICADO, loginService.validarSesion(token));
        return true;
    }
}
