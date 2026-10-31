package com.proyecto.servicios.config;

import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.service.LoginService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SesionInterceptorTest {

    private static final String TOKEN = "3f2b8c1e-9a4d-4c7e-8b21-5d6e7f8a9b0c";

    @Mock
    private LoginService loginService;

    private SesionInterceptor interceptor;
    private MockHttpServletRequest request;

    @BeforeEach
    void setUp() {
        interceptor = new SesionInterceptor(loginService);
        request = new MockHttpServletRequest("POST", "/pagos");
    }

    private void assertError(CatalogoError esperado) {
        assertThatThrownBy(() -> interceptor.preHandle(request, new MockHttpServletResponse(), new Object()))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(esperado));
    }

    @Test
    void sesionValida_dejaPasarYGuardaElClienteAutenticado() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(loginService.validarSesion(TOKEN)).thenReturn(5);

        assertThat(interceptor.preHandle(request, new MockHttpServletResponse(), new Object())).isTrue();
        assertThat(request.getAttribute(SesionInterceptor.CLIENTE_AUTENTICADO)).isEqualTo(5);
    }

    @Test
    void sinCabecera_exigeSesion() {
        assertError(CatalogoError.SESION_REQUERIDA);
        verifyNoInteractions(loginService);
    }

    @Test
    void esquemaDistintoDeBearer_exigeSesion() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Basic dXN1YXJpbzpjbGF2ZQ==");
        assertError(CatalogoError.SESION_REQUERIDA);
        verifyNoInteractions(loginService);
    }

    @Test
    void tokenConFormatoInvalido_seRechazaSinConsultarLaBaseDeDatos() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer ' or 1=1 --");
        assertError(CatalogoError.TOKEN_INVALIDO);
        verifyNoInteractions(loginService);
    }

    @Test
    void sesionExpirada_propagaElErrorDelLogin() {
        request.addHeader(HttpHeaders.AUTHORIZATION, "Bearer " + TOKEN);
        when(loginService.validarSesion(TOKEN)).thenThrow(new NegocioException(CatalogoError.SESION_EXPIRADA, 5));

        assertError(CatalogoError.SESION_EXPIRADA);
        assertThat(request.getAttribute(SesionInterceptor.CLIENTE_AUTENTICADO)).isNull();
    }
}
