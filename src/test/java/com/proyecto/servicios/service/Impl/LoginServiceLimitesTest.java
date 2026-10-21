package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.model.onboarding.LoginRegistroRequest;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.DatosBiometricosLoginRepository;
import com.proyecto.servicios.repositorys.onboarding.LoginRepository;
import com.proyecto.servicios.service.CifradoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

/** Reglas de seguridad del login: bloqueo por intentos fallidos y contraseña vs. usuario. */
@ExtendWith(MockitoExtension.class)
class LoginServiceLimitesTest {

    @Mock
    private LoginRepository loginRepository;
    @Mock
    private DatosBiometricosLoginRepository datosBiometricosLoginRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private CifradoService cifradoService;

    private LoginServiceImpl loginService;

    @BeforeEach
    void setUp() {
        loginService = new LoginServiceImpl(
                loginRepository, datosBiometricosLoginRepository, clienteRepository, passwordEncoder, cifradoService);
    }

    @Test
    void despuesDeCincoIntentosFallidos_elSextoSeBloqueaConHttp429() {
        LoginRequest request = new LoginRequest();
        request.setUsuario("juan.garcia");
        request.setContrasena("Incorrecta1!");

        // loginRepository (mock) no devuelve usuario -> cada intento es un fallo de credenciales
        for (int i = 0; i < 5; i++) {
            assertThatThrownBy(() -> loginService.autenticar(request))
                    .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.CREDENCIALES_INVALIDAS));
        }

        assertThatThrownBy(() -> loginService.autenticar(request))
                .isInstanceOfSatisfying(NegocioException.class,
                        ex -> assertThat(ex.getHttpStatus().value()).isEqualTo(429));
    }

    @Test
    void contrasenaQueContieneElUsuario_seRechazaConCampoContrasena() {
        Cliente cliente = new Cliente();
        cliente.setId(10);
        cliente.setActivo(true);
        when(clienteRepository.findById(10)).thenReturn(Optional.of(cliente));

        LoginRegistroRequest request = new LoginRegistroRequest();
        request.setUsuario("juan.garcia");
        request.setContrasena("Mi-Juan.Garcia-2024!");

        assertThatThrownBy(() -> loginService.registrarAcceso(10, request))
                .isInstanceOfSatisfying(NegocioException.class, ex -> {
                    assertThat(ex.getCampo()).isEqualTo("contrasena");
                    assertThat(ex.getMessage()).contains("nombre de usuario");
                });
    }
}
