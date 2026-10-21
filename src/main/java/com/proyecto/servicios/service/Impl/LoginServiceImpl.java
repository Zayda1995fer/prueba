package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.DatosBiometricosLogin;
import com.proyecto.servicios.entity.onboarding.Login;
import com.proyecto.servicios.model.onboarding.LoginRegistroRequest;
import com.proyecto.servicios.model.onboarding.LoginRequest;
import com.proyecto.servicios.model.onboarding.LoginResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.DatosBiometricosLoginRepository;
import com.proyecto.servicios.repositorys.onboarding.LoginRepository;
import com.proyecto.servicios.service.CifradoService;
import com.proyecto.servicios.service.LoginService;
import com.proyecto.servicios.util.LimitadorIntentosLogin;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.Duration;
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

    // Máximo de intentos fallidos seguidos por usuario y tiempo de bloqueo
    private static final int MAX_INTENTOS_FALLIDOS = 5;
    private static final Duration DURACION_BLOQUEO = Duration.ofMinutes(15);

    private final LimitadorIntentosLogin limitadorIntentos =
            new LimitadorIntentosLogin(MAX_INTENTOS_FALLIDOS, DURACION_BLOQUEO);

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
                .orElseThrow(() -> new NegocioException(CatalogoError.CLIENTE_NO_ENCONTRADO, "id " + clienteId));

        if (!Boolean.TRUE.equals(cliente.getActivo())) {
            throw new NegocioException(CatalogoError.CLIENTE_INACTIVO);
        }
        if (loginRepository.findByClienteId(clienteId).isPresent()) {
            throw new NegocioException(CatalogoError.CLIENTE_CON_ACCESO);
        }
        if (loginRepository.existsByUsuario(request.getUsuario())) {
            throw new NegocioException(CatalogoError.USUARIO_EN_USO, request.getUsuario());
        }
        if (request.getContrasena().toLowerCase().contains(request.getUsuario().toLowerCase())) {
            throw new NegocioException(CatalogoError.CONTRASENA_CONTIENE_USUARIO);
        }

        // 1) Tabla login: solo identidad
        Login login = new Login();
        login.setClienteId(clienteId);
        login.setUsuario(request.getUsuario());
        login.setContrasena(passwordEncoder.encode(request.getContrasena()));
        login.setActivo(true);
        login = loginRepository.save(login);

        // 2) Tabla datos_biometricos_login: el dato sensible, cifrado antes de guardarse
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

        // Si el usuario acumuló demasiados intentos fallidos, ni siquiera se consulta la base
        limitadorIntentos.verificarNoBloqueado(request.getUsuario());

        Login login = loginRepository.findByUsuario(request.getUsuario()).orElse(null);
        if (login == null) {
            limitadorIntentos.registrarFallo(request.getUsuario());
            throw new NegocioException(CatalogoError.CREDENCIALES_INVALIDAS);
        }

        if (!Boolean.TRUE.equals(login.getActivo())) {
            log.error("Intento de acceso con usuario inactivo, usuario={}", request.getUsuario());
            limitadorIntentos.registrarFallo(request.getUsuario());
            throw new NegocioException(CatalogoError.ACCESO_INACTIVO);
        }

        if (!passwordEncoder.matches(request.getContrasena(), login.getContrasena())) {
            log.error("Intento de acceso con contraseña incorrecta, usuario={}", request.getUsuario());
            limitadorIntentos.registrarFallo(request.getUsuario());
            throw new NegocioException(CatalogoError.CREDENCIALES_INVALIDAS);
        }
        limitadorIntentos.registrarExito(request.getUsuario());

        DatosBiometricosLogin datosBiometricos = datosBiometricosLoginRepository.findByLoginId(login.getId())
                .orElseGet(() -> {
                    DatosBiometricosLogin nuevo = new DatosBiometricosLogin();
                    nuevo.setLoginId(login.getId());
                    return nuevo;
                });

        // Se genera el token en texto plano (esto es lo único que se le
        // entrega al cliente); en la base de datos solo quedan su versión
        // cifrada (por si se necesita auditar) y su hash (para buscarlo).
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
                .orElseThrow(() -> new NegocioException(CatalogoError.TOKEN_INVALIDO));

        long minutosSinActividad = ChronoUnit.MINUTES.between(
                datosBiometricos.getUltimaActividad(), LocalDateTime.now());

        if (minutosSinActividad >= sessionTimeoutMinutes) {
            log.error("Sesión expirada, loginId={}, minutosSinActividad={}",
                    datosBiometricos.getLoginId(), minutosSinActividad);
            throw new NegocioException(CatalogoError.SESION_EXPIRADA, sessionTimeoutMinutes);
        }

        // Sesión "deslizante": cada uso válido renueva la ventana de 5 minutos
        datosBiometricos.setUltimaActividad(LocalDateTime.now());
        datosBiometricosLoginRepository.save(datosBiometricos);

        Login login = loginRepository.findById(datosBiometricos.getLoginId())
                .orElseThrow(() -> new NegocioException(CatalogoError.TOKEN_INVALIDO));

        return login.getClienteId();
    }

    @Override
    public void cambiarEstatusAcceso(Integer clienteId, boolean activo) {
        Login login = loginRepository.findByClienteId(clienteId)
                .orElseThrow(() -> new NegocioException(CatalogoError.CLIENTE_NO_ENCONTRADO, "acceso del cliente id " + clienteId));
        login.setActivo(activo);
        loginRepository.save(login);
        log.info("Estatus de acceso actualizado, clienteId={}, activo={}", clienteId, activo);
    }
}
