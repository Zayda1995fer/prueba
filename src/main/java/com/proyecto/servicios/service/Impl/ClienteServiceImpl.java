package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Nacionalidad;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.onboarding.*;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.DomicilioRepository;
import com.proyecto.servicios.repositorys.onboarding.NacionalidadRepository;
import com.proyecto.servicios.service.ClienteService;
import com.proyecto.servicios.model.onboarding.NacionalidadRequest;
import com.proyecto.servicios.util.CurpRfcValidator;
import com.proyecto.servicios.util.NacionalidadNombre;
import com.proyecto.servicios.validation.ReglasValidacion;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.List;
import java.util.Locale;

@Service
@Slf4j
public class ClienteServiceImpl implements ClienteService {

    private static final int EDAD_MINIMA = 18;
    private static final int EDAD_MAXIMA = ReglasValidacion.EDAD_MAXIMA;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final ClienteRepository clienteRepository;
    private final DomicilioRepository domicilioRepository;
    private final CuentaRepository cuentaRepository;
    private final NacionalidadRepository nacionalidadRepository;

    // Saldo con el que el sistema abre toda cuenta nueva. Se deja como
    // propiedad configurable (application.properties) en vez de un
    // número fijo en el código, siguiendo la misma idea que ya usaba
    // el proyecto con gestopago.auth.refresh-rate-ms.
    @Value("${app.cuenta.saldo-inicial:0.00}")
    private BigDecimal saldoInicial;

    public ClienteServiceImpl(ClienteRepository clienteRepository,
                              DomicilioRepository domicilioRepository,
                              CuentaRepository cuentaRepository,
                              NacionalidadRepository nacionalidadRepository) {
        this.clienteRepository = clienteRepository;
        this.domicilioRepository = domicilioRepository;
        this.cuentaRepository = cuentaRepository;
        this.nacionalidadRepository = nacionalidadRepository;
    }

    @Override
    public ClienteResponse registrarCliente(ClienteRequest request) {
        log.info("Iniciando registro de cliente, curp={}", request.getCurp());

        validarEdad(request.getFechaNacimiento());
        validarTelefonosDistintos(request.getTelefonoMovil(), request.getTelefonoAlternativo());
        validarCurpYRfcCoincidenConDatos(request);
        validarNoDuplicado(request);
        Integer nacionalidadId = resolverNacionalidad(request.getNacionalidad());

        Cliente cliente = new Cliente();
        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setFechaNacimiento(request.getFechaNacimiento());
        cliente.setCurp(request.getCurp());
        cliente.setRfc(request.getRfc());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidadId(nacionalidadId);
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setCorreoElectronico(request.getCorreoElectronico());
        cliente.setTelefonoMovil(request.getTelefonoMovil());
        cliente.setTelefonoAlternativo(request.getTelefonoAlternativo());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(normalizarMonto(request.getIngresoMensual()));
        cliente.setActivo(true);
        cliente = clienteRepository.save(cliente);

        Domicilio domicilio = mapearDomicilio(request.getDomicilio(), cliente.getId());
        domicilio = domicilioRepository.save(domicilio);

        // "Creación automática de cuenta" al registrar correctamente al cliente
        Cuenta cuenta = crearCuentaParaCliente(cliente.getId());

        log.info("Registro de cliente finalizado, clienteId={}, numeroCuenta={}",
                cliente.getId(), cuenta.getNumeroCuenta());

        return mapearResponse(cliente, domicilio, cuenta, true);
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
        Cliente cliente = clienteRepository.findByCurp(curp.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ClienteNoEncontradoException("CURP " + curp));
        return armarResponseCompleto(cliente);
    }

    @Override
    public ClienteResponse consultarPorRfc(String rfc) {
        Cliente cliente = clienteRepository.findByRfc(rfc.toUpperCase(Locale.ROOT))
                .orElseThrow(() -> new ClienteNoEncontradoException("RFC " + rfc));
        return armarResponseCompleto(cliente);
    }

    @Override
    public ClienteResponse consultarPorCorreo(String correo) {
        Cliente cliente = clienteRepository.findByCorreoElectronico(correo.toLowerCase(Locale.ROOT))
                .orElseThrow(() -> new ClienteNoEncontradoException("correo " + correo));
        return armarResponseCompleto(cliente);
    }

    @Override
    public ClienteResponse consultarPorNumeroCuenta(String numeroCuenta) {
        Cuenta cuenta = cuentaRepository.findByNumeroCuenta(numeroCuenta)
                .orElseThrow(() -> new ClienteNoEncontradoException("cuenta " + numeroCuenta));
        Cliente cliente = obtenerClienteOLanzar(cuenta.getClienteId());
        Domicilio domicilio = domicilioRepository.findByClienteId(cliente.getId()).orElse(null);
        return mapearResponse(cliente, domicilio, cuenta, false);
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

        Integer nacionalidadId = resolverNacionalidad(request.getNacionalidad());
        validarTelefonosDistintos(request.getTelefonoMovil(), request.getTelefonoAlternativo());

        // Al cambiar nombre, apellidos o sexo, la CURP y el RFC que ya tiene
        // el cliente (que no se pueden modificar) deben seguir coincidiendo.
        CurpRfcValidator.validar(
                request.getNombre(), request.getApellidoPaterno(), request.getApellidoMaterno(),
                cliente.getFechaNacimiento(), request.getSexo(), cliente.getCurp(), cliente.getRfc());

        // CURP, RFC y número de cuenta nunca se tocan aquí: el DTO de
        // actualización ni siquiera trae esos campos.
        cliente.setNombre(request.getNombre());
        cliente.setSegundoNombre(request.getSegundoNombre());
        cliente.setApellidoPaterno(request.getApellidoPaterno());
        cliente.setApellidoMaterno(request.getApellidoMaterno());
        cliente.setSexo(request.getSexo());
        cliente.setNacionalidadId(nacionalidadId);
        cliente.setEstadoCivil(request.getEstadoCivil());
        cliente.setOcupacion(request.getOcupacion());
        cliente.setEmpresa(request.getEmpresa());
        cliente.setIngresoMensual(normalizarMonto(request.getIngresoMensual()));

        // El correo sí se puede actualizar, pero sigue debiendo ser único
        if (!cliente.getCorreoElectronico().equalsIgnoreCase(request.getCorreoElectronico())
                && clienteRepository.existsByCorreoElectronico(request.getCorreoElectronico())) {
            throw new ClienteYaRegistradoException(
                    "Ya existe un cliente registrado con el correo " + request.getCorreoElectronico(),
                    "correoElectronico");
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
        return mapearResponse(cliente, domicilio, cuenta, true);
    }

    @Override
    public void darDeBajaCliente(Integer id) {
        log.info("Iniciando baja lógica de cliente, clienteId={}", id);

        Cliente cliente = obtenerClienteOLanzar(id);
        cliente.setActivo(false);
        clienteRepository.save(cliente);

        // Regla de negocio: "Solo los clientes activos podrán tener cuentas activas"
        cuentaRepository.findByClienteId(id).ifPresent(cuenta -> {
            cuenta.setEstatus(Cuenta.ESTATUS_INACTIVA);
            cuentaRepository.save(cuenta);
        });

        log.info("Baja lógica de cliente finalizada, clienteId={}", id);
    }

    // ------------------------------------------------------------------
    // Métodos de apoyo (privados): aquí se evita repetir código entre
    // los distintos métodos públicos de arriba.
    // ------------------------------------------------------------------

    private void validarEdad(LocalDate fechaNacimiento) {
        int edad = Period.between(fechaNacimiento, LocalDate.now()).getYears();
        if (edad < EDAD_MINIMA) {
            throw new ValidacionException("El cliente debe ser mayor de edad (18 años o más)", "fechaNacimiento");
        }
        if (edad > EDAD_MAXIMA) {
            throw new ValidacionException(
                    "La fecha de nacimiento no es válida: la edad no puede ser mayor a " + EDAD_MAXIMA + " años",
                    "fechaNacimiento");
        }
    }

    // Teléfono alternativo opcional, pero si se captura no puede ser el mismo número que el móvil
    private void validarTelefonosDistintos(Long movil, Long alternativo) {
        if (alternativo != null && alternativo.equals(movil)) {
            throw new ValidacionException(
                    "El teléfono alternativo no puede ser igual al teléfono móvil", "telefonoAlternativo");
        }
    }

    private void validarCurpYRfcCoincidenConDatos(ClienteRequest request) {
        CurpRfcValidator.validar(
                request.getNombre(),
                request.getApellidoPaterno(),
                request.getApellidoMaterno(),
                request.getFechaNacimiento(),
                request.getSexo(),
                request.getCurp(),
                request.getRfc());
    }

    // "Que a la cantidad que se ingrese se le agregue por defecto .00":
    // si capturan 15000 (sin decimales) o 15000.5, se guarda siempre con
    // 2 decimales exactos (15000.00 / 15000.50). @Digits(fraction = 2)
    // en el DTO ya rechaza algo como 15000.555 antes de llegar aquí.
    private BigDecimal normalizarMonto(BigDecimal monto) {
        return monto.setScale(2, RoundingMode.HALF_UP);
    }

    // La nacionalidad llega como arreglo de UN objeto {id, nombre}. Se valida que
    // el id exista en el catálogo y que el nombre corresponda a ese id (acepta
    // masculino/femenino, mayúsculas y acentos: Mexicana, Mexicano, MEXICANA).
    private Integer resolverNacionalidad(List<NacionalidadRequest> nacionalidades) {
        NacionalidadRequest capturada = nacionalidades.get(0);
        Nacionalidad catalogo = nacionalidadRepository.findById(capturada.getId())
                .orElseThrow(() -> new ValidacionException(
                        "La nacionalidad indicada (id " + capturada.getId()
                                + ") no existe en el catálogo. Consulta GET /nacionalidades para ver las nacionalidades disponibles.",
                        "nacionalidad[0].id"));
        if (!NacionalidadNombre.equivalentes(capturada.getNombre(), catalogo.getNombre())) {
            throw new ValidacionException(
                    "El nombre de la nacionalidad \"" + capturada.getNombre() + "\" no corresponde al id "
                            + capturada.getId() + " del catálogo (\"" + catalogo.getNombre()
                            + "\"). Puede escribirse en masculino o femenino, sin importar mayúsculas ni acentos.",
                    "nacionalidad[0].nombre");
        }
        return catalogo.getId();
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
                    "Ya existe un cliente registrado con el correo " + request.getCorreoElectronico(),
                    "correoElectronico");
        }
    }

    private Cuenta crearCuentaParaCliente(Integer clienteId) {
        if (saldoInicial.compareTo(BigDecimal.ZERO) < 0) {
            // Defensa adicional: el saldo inicial tampoco puede ser negativo,
            // ni siquiera si alguien cambia mal la configuración.
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
            // 10 dígitos numéricos, igual que un número de cuenta bancario simple
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
        return mapearResponse(cliente, domicilio, cuenta, false);
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

    private List<NacionalidadResponse> mapearNacionalidad(Integer nacionalidadId) {
        Nacionalidad nacionalidad = nacionalidadRepository.findById(nacionalidadId)
                .orElseThrow(() -> new ValidacionException(
                        "El cliente tiene un nacionalidad_id (" + nacionalidadId + ") que ya no existe en el catálogo"));
        NacionalidadResponse response = new NacionalidadResponse();
        response.setId(nacionalidad.getId());
        response.setNombre(nacionalidad.getNombre());
        return List.of(response);
    }

    // incluirIds = true solo al dar de alta o actualizar; en las consultas no se regresa el id.
    private ClienteResponse mapearResponse(Cliente cliente, Domicilio domicilio, Cuenta cuenta, boolean incluirIds) {
        ClienteResponse response = new ClienteResponse();
        if (incluirIds) {
            response.setId(cliente.getId());
        }
        response.setNombre(cliente.getNombre());
        response.setSegundoNombre(cliente.getSegundoNombre());
        response.setApellidoPaterno(cliente.getApellidoPaterno());
        response.setApellidoMaterno(cliente.getApellidoMaterno());
        response.setFechaNacimiento(cliente.getFechaNacimiento());
        response.setCurp(cliente.getCurp());
        response.setRfc(cliente.getRfc());
        response.setSexo(cliente.getSexo());
        response.setNacionalidad(mapearNacionalidad(cliente.getNacionalidadId()));
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
            response.setCuenta(incluirIds ? mapearCuentaConIds(cuenta) : mapearCuenta(cuenta));
        }

        return response;
    }

    // Método estático (sin estado) para que CuentaServiceImpl también lo
    // use y así no se repita esta conversión Cuenta -> CuentaResponse
    // en dos archivos distintos.
    // Para consultas: sin los ids internos.
    public static CuentaResponse mapearCuenta(Cuenta cuenta) {
        CuentaResponse cuentaResponse = new CuentaResponse();
        cuentaResponse.setNumeroCuenta(cuenta.getNumeroCuenta());
        cuentaResponse.setSaldo(cuenta.getSaldo());
        cuentaResponse.setEstatus(cuenta.getEstatus());
        cuentaResponse.setFechaApertura(cuenta.getFechaApertura());
        return cuentaResponse;
    }

    // Para el alta/actualización: incluye los ids de la cuenta y del cliente.
    public static CuentaResponse mapearCuentaConIds(Cuenta cuenta) {
        CuentaResponse cuentaResponse = mapearCuenta(cuenta);
        cuentaResponse.setId(cuenta.getId());
        cuentaResponse.setClienteId(cuenta.getClienteId());
        return cuentaResponse;
    }
}