package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.DatosBiometricosLogin;
import com.proyecto.servicios.entity.onboarding.Login;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.SesionExpiradaException;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static reactor.core.publisher.Mono.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

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
        ReflectionTestUtils.setField(loginService, "sessionTimeoutMinutes", 5L);

        when(cifradoService.cifrar(anyString())).thenAnswer(inv -> "CIFRADO(" + inv.getArgument(0) + ")");
        when(cifradoService.hashParaBusqueda(anyString())).thenAnswer(inv -> "HASH(" + inv.getArgument(0) + ")");
    }

    private Login loginDeEjemplo() {
        Login login = new Login();
        login.setId(1);
        login.setClienteId(10);
        login.setUsuario("juan.garcia");
        login.setContrasena("hash-guardado");
        login.setActivo(true);
        return login;
    }

    @Test
    void autenticar_conCredencialesCorrectas_devuelveTokenPlanoYGuardaCifrado() {
        Login login = loginDeEjemplo();
        LoginRequest request = new LoginRequest();
        request.setUsuario("juan.garcia");
        request.setContrasena("Password123");

        when(loginRepository.findByUsuario("juan.garcia")).thenReturn(Optional.of(login));
        when(passwordEncoder.matches("Password123", "hash-guardado")).thenReturn(true);
        when(datosBiometricosLoginRepository.findByLoginId(1)).thenReturn(Optional.empty());
        when(datosBiometricosLoginRepository.save(any(DatosBiometricosLogin.class)))
                .thenAnswer(inv -> inv.getArgument(0));

        LoginResponse response = loginService.autenticar(request);

        assertThat(response.getTokenSesion()).isNotBlank();
        assertThat(response.getDuracionSesionMinutos()).isEqualTo(5);

        var captor = org.mockito.ArgumentCaptor.forClass(DatosBiometricosLogin.class);
        org.mockito.Mockito.verify(datosBiometricosLoginRepository).save(captor.capture());
        assertThat(captor.getValue().getTokenSesionCifrado()).startsWith("CIFRADO(");
        assertThat(captor.getValue().getTokenSesionHash()).startsWith("HASH(");
        assertThat(captor.getValue().getTokenSesionCifrado()).isNotEqualTo(response.getTokenSesion());
    }

    @Test
    void autenticar_usuarioNoExiste_lanzaCredencialesInvalidas() {
        LoginRequest request = new LoginRequest();
        request.setUsuario("no-existe");
        request.setContrasena("cualquiera");

        when(loginRepository.findByUsuario("no-existe")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.autenticar(request))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void autenticar_contrasenaIncorrecta_lanzaCredencialesInvalidas() {
        Login login = loginDeEjemplo();
        LoginRequest request = new LoginRequest();
        request.setUsuario("juan.garcia");
        request.setContrasena("incorrecta");

        when(loginRepository.findByUsuario("juan.garcia")).thenReturn(Optional.of(login));
        when(passwordEncoder.matches("incorrecta", "hash-guardado")).thenReturn(false);

        assertThatThrownBy(() -> loginService.autenticar(request))
                .isInstanceOf(CredencialesInvalidasException.class);
    }

    @Test
    void autenticar_accesoInactivo_lanzaCredencialesInvalidas() {
        Login login = loginDeEjemplo();
        login.setActivo(false);
        LoginRequest request = new LoginRequest();
        request.setUsuario("juan.garcia");
        request.setContrasena("Password123");

        when(loginRepository.findByUsuario("juan.garcia")).thenReturn(Optional.of(login));

        assertThatThrownBy(() -> loginService.autenticar(request))
                .isInstanceOf(CredencialesInvalidasException.class)
                .hasMessageContaining("inactivo");
    }

    @Test
    void validarSesion_dentroDelLimiteDeCincoMinutos_renuevaYRegresaClienteId() {
        DatosBiometricosLogin datos = new DatosBiometricosLogin();
        datos.setLoginId(1);
        datos.setTokenSesionHash("HASH(token-123)");
        datos.setUltimaActividad(LocalDateTime.now().minusMinutes(2));

        Login login = loginDeEjemplo();

        when(datosBiometricosLoginRepository.findByTokenSesionHash("HASH(token-123)"))
                .thenReturn(Optional.of(datos));
        when(datosBiometricosLoginRepository.save(datos)).thenReturn(datos);
        when(loginRepository.findById(1)).thenReturn(Optional.of(login));

        Integer clienteId = loginService.validarSesion("token-123");

        assertThat(clienteId).isEqualTo(10);
    }

    @Test
    void validarSesion_pasaronMasDeCincoMinutos_lanzaSesionExpirada() {
        DatosBiometricosLogin datos = new DatosBiometricosLogin();
        datos.setLoginId(1);
        datos.setTokenSesionHash("HASH(token-123)");
        datos.setUltimaActividad(LocalDateTime.now().minusMinutes(6));

        when(datosBiometricosLoginRepository.findByTokenSesionHash("HASH(token-123)"))
                .thenReturn(Optional.of(datos));

        assertThatThrownBy(() -> loginService.validarSesion("token-123"))
                .isInstanceOf(SesionExpiradaException.class);
    }

    @Test
    void validarSesion_tokenInexistente_lanzaCredencialesInvalidas() {
        when(datosBiometricosLoginRepository.findByTokenSesionHash("HASH(token-falso)"))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> loginService.validarSesion("token-falso"))
                .isInstanceOf(CredencialesInvalidasException.class);
    }
}
