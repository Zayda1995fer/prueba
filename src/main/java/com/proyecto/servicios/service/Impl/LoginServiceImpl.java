package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.DatosBiometricosLogin;
import com.proyecto.servicios.entity.onboarding.Login;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CredencialesInvalidasException;
import com.proyecto.servicios.exception.SesionExpiradaException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.onboarding.LoginRegistroRequest;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.DatosBiometricosLoginRepository;
import com.proyecto.servicios.repositorys.onboarding.LoginRepository;
import com.proyecto.servicios.service.CifradoService;
import com.proyecto.servicios.service.LoginService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

@Service
@Slf4j
public class LoginServiceImpl implements LoginService {

    private final LoginRepository loginRepository;
    private final DatosBiometricosLoginRepository datosBiometricosLoginRepository;
    private final ClienteRepository clienteRepository;
    private final PasswordEncoder passwordEncoder;
    private final CifradoService cifradoService;

    @Value("${app.login.session-timeout-minutes:5}")
    private long sessionTimeoutMinutes;

    public LoginServiceImpl(LoginRepository loginRepository,
                            DatosBiometricosLoginRepository datosBiometricosLoginRepository,
                            ClienteRepository clienteRepository,
                            PasswordEncoder passwordEncoder,
                            CifradoService cifradoService) {
        this.loginRepository = loginRepository;
        this.datosBiometricosLoginRepository = datosBiometricosLoginRepository;
        this.clienteRepository = clienteRepository;
        this.passwordEncoder = passwordEncoder;
        this.cifradoService = cifradoService;
    }

    @Override
    public void registrarAcceso(Integer clienteId, LoginRegistroRequest request) {
        log.info("Iniciando registro de acceso para clienteId={}", clienteId);

        Cliente cliente = clienteRepository.findById(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException("id " + clienteId));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new ValidacionException("No se puede crear acceso para un cliente inactivo");
        }
        if (loginRepository.findByClienteId(clienteId).isPresent()) {
            throw new ClienteYaRegistradoException("El cliente ya cuenta con credenciales de acceso");
        }
        if (loginRepository.existsByUsuario(request.getUsuario())) {
            throw new ClienteYaRegistradoException("El usuario '" + request.getUsuario() + "' ya está en uso");
        }

        Login login = new Login();
        login.setClienteId(clienteId);
        login.setUsuario(request.getUsuario());
        login.setContrasena(passwordEncoder.encode(request.getContrasena()));
        login.setActivo(true);
        login = loginRepository.save(login);

        DatosBiometricosLogin datosBiometricos = new DatosBiometricosLogin();
        datosBiometricos.setLoginId(login.getId());
        if (request.getDatoBiometrico() != null) {
            datosBiometricos.setDatoBiometricoCifrado(
                    cifradoService.cifrar(String.valueOf(request.getDatoBiometrico())));
        }
        datosBiometricosLoginRepository.save(datosBiometricos);

        log.info("Registro de acceso finalizado para clienteId={}", clienteId);
    }

    @Override
    public LoginResponse autenticar(LoginRequest request) {
        log.info("Iniciando autenticación, usuario={}", request.getUsuario());

        Login login = loginRepository.findByUsuario(request.getUsuario())
                .orElseThrow(() -> new CredencialesInvalidasException("Usuario o contraseña incorrectos"));

        if (!Boolean.TRUE.equals(login.getActivo())) {
            log.error("Intento de acceso con usuario inactivo, usuario={}", request.getUsuario());
            throw new CredencialesInvalidasException("El acceso se encuentra inactivo");
        }

        if (!passwordEncoder.matches(request.getContrasena(), login.getContrasena())) {
            log.error("Intento de acceso con contraseña incorrecta, usuario={}", request.getUsuario());
            throw new CredencialesInvalidasException("Usuario o contraseña incorrectos");
        }

        DatosBiometricosLogin datosBiometricos = datosBiometricosLoginRepository.findByLoginId(login.getId())
                .orElseGet(() -> {
                    DatosBiometricosLogin nuevo = new DatosBiometricosLogin();
                    nuevo.setLoginId(login.getId());
                    return nuevo;
                });

        String tokenPlano = UUID.randomUUID().toString();
        datosBiometricos.setTokenSesionCifrado(cifradoService.cifrar(tokenPlano));
        datosBiometricos.setTokenSesionHash(cifradoService.hashParaBusqueda(tokenPlano));
        datosBiometricos.setUltimaActividad(LocalDateTime.now());
        datosBiometricosLoginRepository.save(datosBiometricos);

        log.info("Autenticación finalizada correctamente, usuario={}", request.getUsuario());

        LoginResponse response = new LoginResponse();
        response.setTokenSesion(tokenPlano);
        response.setDuracionSesionMinutos((int) sessionTimeoutMinutes);
        return response;
    }

    @Override
    public Integer validarSesion(String tokenSesion) {
        String hashRecibido = cifradoService.hashParaBusqueda(tokenSesion);

        DatosBiometricosLogin datosBiometricos = datosBiometricosLoginRepository.findByTokenSesionHash(hashRecibido)
                .orElseThrow(() -> new CredencialesInvalidasException("Token de sesión inválido"));

        long minutosSinActividad = ChronoUnit.MINUTES.between(
                datosBiometricos.getUltimaActividad(), LocalDateTime.now());

        if (minutosSinActividad >= sessionTimeoutMinutes) {
            log.error("Sesión expirada, loginId={}, minutosSinActividad={}",
                    datosBiometricos.getLoginId(), minutosSinActividad);
            throw new SesionExpiradaException(
                    "La sesión expiró por inactividad (límite de " + sessionTimeoutMinutes + " minutos)");
        }

        datosBiometricos.setUltimaActividad(LocalDateTime.now());
        datosBiometricosLoginRepository.save(datosBiometricos);

        Login login = loginRepository.findById(datosBiometricos.getLoginId())
                .orElseThrow(() -> new CredencialesInvalidasException("Token de sesión inválido"));

        return login.getClienteId();
    }

    @Override
    public void cambiarEstatusAcceso(Integer clienteId, boolean activo) {
        Login login = loginRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new ClienteNoEncontradoException("acceso del cliente id " + clienteId));
        login.setActivo(activo);
        loginRepository.save(login);
        log.info("Estatus de acceso actualizado, clienteId={}, activo={}", clienteId, activo);
    }
}