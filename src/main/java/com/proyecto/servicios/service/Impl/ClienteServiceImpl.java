package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.onboarding.*;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.DomicilioRepository;
import com.proyecto.servicios.service.ClienteService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final int EDAD_MINIMA = 18;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final CuentaRepository cuentaRepository;

    @Value("${app.cuenta.saldo-inicial:0.00}")
    private BigDecimal saldoInicial;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              DomicilioRepository domicilioRepository,
                              CuentaRepository cuentaRepository) {
        this.clienteRepository = clienteRepository;
        this.domicilioRepository = domicilioRepository;
        this.cuentaRepository = cuentaRepository;
    }

    @Override
    public ClienteResponse registrarCliente(ClienteRequest request) {
        log.info("Iniciando registro de cliente, curp={}", request.getCurp());

        validarMayoriaDeEdad(request.getFechaNacimiento());
        validarNoDuplicado(request);

        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(request.getCurp());
        cliente.setRfc(request.getRfc());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setCorreoElectronico(request.getCorreoElectronico());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());
        cliente.setActivo(true);
        cliente = clienteRepository.save(cliente);

        Domicilio domicilio = mapearDomicilio(request.getDomicilio(), cliente.getId());
        domicilio = domicilioRepository.save(domicilio);

        Cuenta cuenta = crearCuentaParaCliente(cliente.getId());

        log.info("Registro de cliente finalizado, clienteId={}, numeroCuenta={}",
                cliente.getId(), cuenta.getNumeroCuenta());

        return mapearResponse(cliente, domicilio, cuenta);
    }

    @Override
    public List<ClienteResponse> consultarTodos() {
        return clienteRepository.findAll().stream()
                .map(this::armarResponseCompleto)
                .toList();
    }

    @Override
    public ClienteResponse consultarPorId(Integer id) {
        Cliente cliente = obtenerClienteOLanzar(id);
        return armarResponseCompleto(cliente);
    }

    @Override
    public ClienteResponse consultarPorCurp(String curp) {
        Cliente cliente = clienteRepository.findByCurp(curp)
                .orElseThrow(() -> new ClienteNoEncontradoException("CURP " + curp));
        return armarResponseCompleto(cliente);
    }

    @Override
    public ClienteResponse consultarPorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc)
                .orElseThrow(() -> new ClienteNoEncontradoException("RFC " + rfc));
        return armarResponseCompleto(cliente);
    }

    @Override
    public ClienteResponse consultarPorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreoElectronico(correo)
                .orElseThrow(() -> new ClienteNoEncontradoException("correo " + correo));
        return armarResponseCompleto(cliente);
    }

    @Override
    public ClienteResponse consultarPorNumeroCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ClienteNoEncontradoException("cuenta " + numeroCuenta));
        Cliente cliente = obtenerClienteOLanzar(cuenta.getClienteId());
        Domicilio domicilio = domicilioRepository.findByClienteId(cliente.getId()).orElse(null);
        return mapearResponse(cliente, domicilio, cuenta);
    }

    @Override
    public List<ClienteResponse> consultarActivos() {
        return clienteRepository.findByActivoTrue().stream()
                .map(this::armarResponseCompleto)
                .toList();
    }

    @Override
    public List<ClienteResponse> consultarPorRangoDeFechas(LocalDateTime desde, LocalDateTime hasta) {
        if (desde.isAfter(hasta)) {
            throw new ValidacionException("La fecha 'desde' no puede ser posterior a la fecha 'hasta'");
        }
        return clienteRepository.findByFechaCreacionBetween(desde, hasta).stream()
                .map(this::armarResponseCompleto)
                .toList();
    }

    @Override
    public ClienteResponse actualizarCliente(Integer id, ClienteUpdateRequest request) {
        log.info("Iniciando actualización de cliente, clienteId={}", id);

        Cliente cliente = obtenerClienteOLanzar(id);

        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidad(request.getNacionalidad());
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(request.getIngresoMensual());

        if (!cliente.getCorreoElectronico().equalsIgnoreCase(request.getCorreoElectronico())
                && clienteRepository.existsByCorreoElectronico(request.getCorreoElectronico())) {
            throw new ClienteYaRegistradoException(
                    "Ya existe un cliente registrado con el correo " + request.getCorreoElectronico());
        }
        cliente.setCorreoElectronico(request.getCorreoElectronico());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());

        cliente = clienteRepository.save(cliente);

        Domicilio domicilio = domicilioRepository.findByClienteId(id)
                .orElseGet(() -> mapearDomicilio(request.getDomicilio(), id));
        actualizarDatosDomicilio(domicilio, request.getDomicilio());
        domicilio = domicilioRepository.save(domicilio);

        log.info("Actualización de cliente finalizada, clienteId={}", id);

        Cuenta cuenta = cuentaRepository.findByClienteId(id).orElse(null);
        return mapearResponse(cliente, domicilio, cuenta);
    }

    @Override
    public void darDeBajaCliente(Integer id) {
        log.info("Iniciando baja lógica de cliente, clienteId={}", id);

        Cliente cliente = obtenerClienteOLanzar(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);

        cuentaRepository.findByClienteId(id).ifPresent(cuenta -> {
            cuenta.setEstatus(Cuenta.ESTATUS_INACTIVA);
            cuentaRepository.save(cuenta);
        });

        log.info("Baja lógica de cliente finalizada, clienteId={}", id);
    }

    private void validarMayoriaDeEdad(LocalDate fechaNacimiento) {
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new ValidacionException("El cliente debe ser mayor de edad (18 años o más)");
        }
    }

    private void validarNoDuplicado(ClienteRequest request) {
        if (clienteRepository.existsByCurp(request.getCurp())) {
            throw new CurpDuplicadaException(request.getCurp());
        }
        if (clienteRepository.existsByRfc(request.getRfc())) {
            throw new RfcDuplicadoException(request.getRfc());
        }
        if (clienteRepository.existsByCorreoElectronico(request.getCorreoElectronico())) {
            throw new ClienteYaRegistradoException(
                    "Ya existe un cliente registrado con el correo " + request.getCorreoElectronico());
        }
    }

    private Cuenta crearCuentaParaCliente(Integer clienteId) {
        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            throw new ValidacionException("El saldo inicial configurado no puede ser negativo");
        }

        Cuenta cuenta = new Cuenta();
        cuenta.setClienteId(clienteId);
        cuenta.setNumeroCuenta(generarNumeroCuentaUnico());
        cuenta.setSaldo(saldoInicial);
        cuenta.setEstatus(Cuenta.ESTATUS_ACTIVA);
        return cuentaRepository.save(cuenta);
    }

    private String generarNumeroCuentaUnico() {
        String numeroCuenta;
        do {
            numeroCuenta = String.format("%010d", Math.abs(RANDOM.nextLong() % 10_000_000_000L));
        } while (cuentaRepository.existsByNumeroCuenta(numeroCuenta));
        return numeroCuenta;
    }

    private Cliente obtenerClienteOLanzar(Integer id) {
        return clienteRepository.findById(id)
                .orElseThrow(() -> new ClienteNoEncontradoException("id " + id));
    }

    private ClienteResponse armarResponseCompleto(Cliente cliente) {
        Domicilio domicilio = domicilioRepository.findByClienteId(cliente.getId()).orElse(null);
        Cuenta cuenta = cuentaRepository.findByClienteId(cliente.getId()).orElse(null);
        return mapearResponse(cliente, domicilio, cuenta);
    }

    private Domicilio mapearDomicilio(DomicilioRequest request, Integer clienteId) {
        Domicilio domicilio = new Domicilio();
        domicilio.setClienteId(clienteId);
        actualizarDatosDomicilio(domicilio, request);
        return domicilio;
    }

    private void actualizarDatosDomicilio(Domicilio domicilio, DomicilioRequest request) {
        domicilio.setCalle(request.getCalle());
        domicilio.setNumeroExterior(request.getNumeroExterior());
        domicilio.setNumeroInterior(request.getNumeroInterior());
        domicilio.setColonia(request.getColonia());
        domicilio.setMunicipio(request.getMunicipio());
        domicilio.setEstado(request.getEstado());
        domicilio.setCodigoPostal(request.getCodigoPostal());
        domicilio.setPais(request.getPais());
    }

    private ClienteResponse mapearResponse(Cliente cliente, Domicilio domicilio, Cuenta cuenta) {
        ClienteResponse response = new ClienteResponse();
        response.setId(cliente.getId());
        response.setNombre(cliente.getNombre());
        response.setSegundoNombre(cliente.getSegundoNombre());
        response.setApellidoPaterno(cliente.getApellidoPaterno());
        response.setApellidoMaterno(cliente.getApellidoMaterno());
        response.setFechaNacimiento(cliente.getFechaNacimiento());
        response.setCurp(cliente.getCurp());
        response.setRfc(cliente.getRfc());
        response.setSexo(cliente.getSexo());
        response.setNacionalidad(cliente.getNacionalidad());
        response.setEstadoCivil(cliente.getEstadoCivil());
        response.setCorreoElectronico(cliente.getCorreoElectronico());
        response.setTelefonoMovil(cliente.getTelefonoMovil());
        response.setTelefonoAlternativo(cliente.getTelefonoAlternativo());
        response.setOcupacion(cliente.getOcupacion());
        response.setEmpresa(cliente.getEmpresa());
        response.setIngresoMensual(cliente.getIngresoMensual());
        response.setActivo(cliente.getActivo());
        response.setFechaCreacion(cliente.getFechaCreacion());

        if (domicilio != null) {
            DomicilioResponse domicilioResponse = new DomicilioResponse();
            domicilioResponse.setCalle(domicilio.getCalle());
            domicilioResponse.setNumeroExterior(domicilio.getNumeroExterior());
            domicilioResponse.setNumeroInterior(domicilio.getNumeroInterior());
            domicilioResponse.setColonia(domicilio.getColonia());
            domicilioResponse.setMunicipio(domicilio.getMunicipio());
            domicilioResponse.setEstado(domicilio.getEstado());
            domicilioResponse.setCodigoPostal(domicilio.getCodigoPostal());
            domicilioResponse.setPais(domicilio.getPais());
            response.setDomicilio(domicilioResponse);
        }

        if (cuenta != null) {
            response.setCuenta(mapearCuenta(cuenta));
        }

        return response;
    }

    public static CuentaResponse mapearCuenta(Cuenta cuenta) {
        CuentaResponse cuentaResponse = new CuentaResponse();
        cuentaResponse.setId(cuenta.getId());
        cuentaResponse.setClienteId(cuenta.getClienteId());
        cuentaResponse.setNumeroCuenta(cuenta.getNumeroCuenta());
        cuentaResponse.setSaldo(cuenta.getSaldo());
        cuentaResponse.setEstatus(cuenta.getEstatus());
        cuentaResponse.setFechaApertura(cuenta.getFechaApertura());
        return cuentaResponse;
    }
}