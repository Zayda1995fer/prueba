package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Nacionalidad;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.DomicilioRepository;
import com.proyecto.servicios.model.onboarding.ClienteConsultaRequest;
import com.proyecto.servicios.model.onboarding.NacionalidadRequest;
import com.proyecto.servicios.repositorys.onboarding.NacionalidadRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import org.mockito.ArgumentMatchers;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClienteServiceImplTest {

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private DomicilioRepository domicilioRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private NacionalidadRepository nacionalidadRepository;

    private ClienteServiceImpl clienteService;

    @BeforeEach
    void setUp() {
        clienteService = new ClienteServiceImpl(clienteRepository, domicilioRepository, cuentaRepository, nacionalidadRepository);
        // @Value no se inyecta fuera de un contexto Spring; se fija a mano para la prueba
        ReflectionTestUtils.setField(clienteService, "saldoInicial", new BigDecimal("0.00"));
    }

    private ClienteRequest solicitudValida() {
        ClienteRequest request = new ClienteRequest();
        request.setNombre("Juan");
        request.setApellidoPaterno("Garcia");
        request.setApellidoMaterno("Lopez");
        request.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        // CURP/RFC construidas con el algoritmo real a partir de los datos
        // de arriba (Garcia=>G+A de "arcia", Lopez=>L, Juan=>J, 1990-01-01,
        // sexo H), para que pasen la nueva validación de coincidencia.
        request.setCurp("GALJ900101HDFRPN01");
        request.setRfc("GALJ900101A01");
        request.setSexo("H");
        request.setNacionalidad(List.of(nacionalidad(1, "Mexicana")));
        request.setEstadoCivil("Soltero");
        request.setCorreoElectronico("juan.garcia@correo.com");
        request.setTelefonoMovil(5512345678L);
        request.setOcupacion("Ingeniero");
        request.setEmpresa("ACME");
        request.setIngresoMensual(new BigDecimal("15000.00"));

        DomicilioRequest domicilio = new DomicilioRequest();
        domicilio.setCalle("Reforma");
        domicilio.setNumeroExterior("100");
        domicilio.setColonia("Centro");
        domicilio.setMunicipio("Cuauhtémoc");
        domicilio.setEstado("CDMX");
        domicilio.setCodigoPostal("06000");
        domicilio.setPais("México");
        request.setDomicilio(domicilio);

        return request;
    }

    @Test
    void registrarCliente_conDatosValidos_creaClienteDomicilioYCuenta() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(false);
        Nacionalidad mexicana = new Nacionalidad();
        mexicana.setId(1);
        mexicana.setNombre("Mexicana");
        when(nacionalidadRepository.findById(1)).thenReturn(Optional.of(mexicana));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(1);
            return c;
        });
        when(domicilioRepository.save(any(Domicilio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> {
            Cuenta cta = inv.getArgument(0);
            cta.setId(1);
            return cta;
        });

        ClienteResponse response = clienteService.registrarCliente(solicitudValida());

        assertThat(response.getId()).isEqualTo(1);
        assertThat(response.getNacionalidad()).hasSize(1);
        assertThat(response.getNacionalidad().get(0).getId()).isEqualTo(1);
        assertThat(response.getNacionalidad().get(0).getNombre()).isEqualTo("Mexicana");
        assertThat(response.getCuenta()).isNotNull();
        assertThat(response.getCuenta().getEstatus()).isEqualTo(Cuenta.ESTATUS_ACTIVA);
        assertThat(response.getCuenta().getNumeroCuenta()).hasSize(10);

        ArgumentCaptor<Cuenta> cuentaCaptor = ArgumentCaptor.forClass(Cuenta.class);
        verify(cuentaRepository).save(cuentaCaptor.capture());
        assertThat(cuentaCaptor.getValue().getSaldo()).isEqualByComparingTo("0.00");
    }

    @Test
    void registrarCliente_menorDeEdad_lanzaValidacionException() {
        ClienteRequest request = solicitudValida();
        request.setFechaNacimiento(LocalDate.now().minusYears(10));

        assertThatThrownBy(() -> clienteService.registrarCliente(request))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.CLIENTE_MENOR_DE_EDAD))
                .hasMessageContaining("mayor de edad");

        verifyNoInteractions(cuentaRepository);
    }

    @Test
    void registrarCliente_curpDuplicada_lanzaCurpDuplicadaException() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.registrarCliente(solicitudValida()))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.CURP_DUPLICADA));
    }

    @Test
    void registrarCliente_rfcDuplicado_lanzaRfcDuplicadoException() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.registrarCliente(solicitudValida()))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.RFC_DUPLICADO));
    }

    @Test
    void registrarCliente_correoDuplicado_lanzaClienteYaRegistradoException() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.registrarCliente(solicitudValida()))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.CORREO_DUPLICADO));
    }

    @Test
    void registrarCliente_nacionalidadNoExisteEnCatalogo_lanzaValidacionException() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(false);
        when(nacionalidadRepository.findById(999)).thenReturn(Optional.empty());

        ClienteRequest request = solicitudValida();
        request.setNacionalidad(List.of(nacionalidad(999, "Mexicana")));

        assertThatThrownBy(() -> clienteService.registrarCliente(request))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.NACIONALIDAD_NO_EXISTE))
                .hasMessageContaining("catálogo");

        verifyNoInteractions(cuentaRepository);
    }

    @Test
    void registrarCliente_nombreDeNacionalidadNoCorrespondeAlId_lanzaValidacionException() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(false);
        Nacionalidad mexicana = new Nacionalidad();
        mexicana.setId(1);
        mexicana.setNombre("Mexicana");
        when(nacionalidadRepository.findById(1)).thenReturn(Optional.of(mexicana));

        ClienteRequest request = solicitudValida();
        request.setNacionalidad(List.of(nacionalidad(1, "Estadounidense")));

        assertThatThrownBy(() -> clienteService.registrarCliente(request))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.NACIONALIDAD_NOMBRE_NO_COINCIDE))
                .hasMessageContaining("no corresponde al id 1");
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    void registrarCliente_nacionalidadEnMasculino_seAcepta() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(false);
        Nacionalidad mexicana = new Nacionalidad();
        mexicana.setId(1);
        mexicana.setNombre("Mexicana");
        when(nacionalidadRepository.findById(1)).thenReturn(Optional.of(mexicana));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> {
            Cliente c = inv.getArgument(0);
            c.setId(1);
            return c;
        });
        when(domicilioRepository.save(any(Domicilio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaRepository.existsByNumeroCuenta(anyString())).thenReturn(false);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteRequest request = solicitudValida();
        request.setNacionalidad(List.of(nacionalidad(1, "MEXICANO")));

        ClienteResponse response = clienteService.registrarCliente(request);

        assertThat(response.getNacionalidad().get(0).getNombre()).isEqualTo("Mexicana");
    }

    private static NacionalidadRequest nacionalidad(int id, String nombre) {
        NacionalidadRequest n = new NacionalidadRequest();
        n.setId(id);
        n.setNombre(nombre);
        return n;
    }

    @Test
    void consultarClientes_noRegresaElId() {
        Cliente cliente = new Cliente();
        cliente.setId(7);
        cliente.setNombre("Juan");
        cliente.setNacionalidadId(1);
        cliente.setIngresoMensual(new java.math.BigDecimal("15000.00"));
        Nacionalidad mexicana = new Nacionalidad();
        mexicana.setId(1);
        mexicana.setNombre("Mexicana");
        when(clienteRepository.findAll(ArgumentMatchers.<Specification<Cliente>>any(), any(Sort.class)))
                .thenReturn(List.of(cliente));
        when(nacionalidadRepository.findById(1)).thenReturn(Optional.of(mexicana));
        when(domicilioRepository.findByClienteId(7)).thenReturn(Optional.empty());
        when(cuentaRepository.findByClienteId(7)).thenReturn(Optional.empty());

        ClienteConsultaRequest criterios = new ClienteConsultaRequest();
        criterios.setId(7);
        List<ClienteResponse> respuesta = clienteService.consultarClientes(criterios);

        assertThat(respuesta).hasSize(1);
        assertThat(respuesta.get(0).getId()).isNull();
    }

    @Test
    void consultarClientes_porDatoUnicoQueNoExiste_lanzaClienteNoEncontradoException() {
        when(clienteRepository.findAll(ArgumentMatchers.<Specification<Cliente>>any(), any(Sort.class)))
                .thenReturn(List.of());

        ClienteConsultaRequest criterios = new ClienteConsultaRequest();
        criterios.setId(99);

        assertThatThrownBy(() -> clienteService.consultarClientes(criterios))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.CLIENTE_NO_ENCONTRADO));
    }

    @Test
    void consultarClientes_filtroGeneralSinResultados_regresaListaVacia() {
        when(clienteRepository.findAll(ArgumentMatchers.<Specification<Cliente>>any(), any(Sort.class)))
                .thenReturn(List.of());

        ClienteConsultaRequest criterios = new ClienteConsultaRequest();
        criterios.setNombre("Zacarias");
        criterios.setActivo(true);

        assertThat(clienteService.consultarClientes(criterios)).isEmpty();
    }

    @Test
    void consultarClientes_fechaDesdePosteriorAHasta_lanzaValidacionConCampo() {
        ClienteConsultaRequest criterios = new ClienteConsultaRequest();
        criterios.setFechaCreacionDesde(java.time.LocalDate.of(2026, 10, 10));
        criterios.setFechaCreacionHasta(java.time.LocalDate.of(2026, 10, 1));

        assertThatThrownBy(() -> clienteService.consultarClientes(criterios))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.RANGO_FECHAS_INVALIDO))
                .satisfies(ex -> assertThat(((NegocioException) ex).getCampo()).isEqualTo("fechaCreacionDesde"));
        verifyNoInteractions(clienteRepository);
    }

    @Test
    void consultarClientes_numeroDeCuentaInexistente_lanzaCuentaNoEncontradaException() {
        when(cuentaRepository.findByNumeroCuenta("0000000000")).thenReturn(Optional.empty());

        ClienteConsultaRequest criterios = new ClienteConsultaRequest();
        criterios.setNumeroCuenta("0000000000");

        assertThatThrownBy(() -> clienteService.consultarClientes(criterios))
                .isInstanceOfSatisfying(NegocioException.class, ex -> {
                    assertThat(ex.getError()).isEqualTo(CatalogoError.CUENTA_NO_ENCONTRADA);
                    assertThat(ex.getMessage()).isEqualTo("No se encontró la cuenta con número: 0000000000");
                });
    }

    private ClienteUpdateRequest actualizacionValida(Integer id) {
        ClienteUpdateRequest u = new ClienteUpdateRequest();
        u.setId(id);
        u.setNombre("Juan");
        u.setApellidoPaterno("Garcia");
        u.setApellidoMaterno("Lopez");
        u.setSexo("H");
        u.setNacionalidad(List.of(nacionalidad(1, "Mexicana")));
        u.setEstadoCivil("Soltero");
        u.setCorreoElectronico("juan.garcia@correo.com");
        u.setTelefonoMovil(5512345678L);
        u.setOcupacion("Ingeniero");
        u.setEmpresa("ACME");
        u.setIngresoMensual(new BigDecimal("15000.00"));
        u.setDomicilio(solicitudValida().getDomicilio());
        return u;
    }

    private Cliente clienteExistente(Integer id) {
        Cliente cliente = new Cliente();
        cliente.setId(id);
        cliente.setNombre("Juan");
        cliente.setApellidoPaterno("Garcia");
        cliente.setApellidoMaterno("Lopez");
        cliente.setFechaNacimiento(LocalDate.of(1990, 1, 1));
        cliente.setCurp("GALJ900101HDFRPN01");
        cliente.setRfc("GALJ900101A01");
        cliente.setSexo("H");
        cliente.setNacionalidadId(1);
        cliente.setCorreoElectronico("juan.garcia@correo.com");
        cliente.setActivo(true);
        return cliente;
    }

    private void simularRepositoriosDeActualizacion(Cliente cliente, Cuenta cuenta) {
        Nacionalidad mexicana = new Nacionalidad();
        mexicana.setId(1);
        mexicana.setNombre("Mexicana");
        when(nacionalidadRepository.findById(1)).thenReturn(Optional.of(mexicana));
        when(clienteRepository.findById(cliente.getId())).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(domicilioRepository.findByClienteId(cliente.getId())).thenReturn(Optional.empty());
        when(domicilioRepository.save(any(Domicilio.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaRepository.findByClienteId(cliente.getId())).thenReturn(Optional.of(cuenta));
    }

    private Cuenta cuentaExistente(Integer clienteId) {
        Cuenta cuenta = new Cuenta();
        cuenta.setId(9);
        cuenta.setClienteId(clienteId);
        cuenta.setNumeroCuenta("1234567890");
        cuenta.setSaldo(new BigDecimal("0.00"));
        cuenta.setEstatus(Cuenta.ESTATUS_ACTIVA);
        cuenta.setFechaApertura(java.time.LocalDateTime.now());
        return cuenta;
    }

    @Test
    void actualizarCliente_conActivoFalse_daDeBajaAlClienteYDejaInactivaSuCuenta() {
        Cliente cliente = clienteExistente(5);
        Cuenta cuenta = cuentaExistente(5);
        simularRepositoriosDeActualizacion(cliente, cuenta);
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        ClienteUpdateRequest request = actualizacionValida(5);
        request.setActivo(false);
        ClienteResponse response = clienteService.actualizarCliente(request);

        assertThat(cliente.getActivo()).isFalse();
        assertThat(cuenta.getEstatus()).isEqualTo(Cuenta.ESTATUS_INACTIVA);
        assertThat(response.getActivo()).isFalse();
    }

    @Test
    void actualizarCliente_sinCampoActivo_noCambiaElEstatus() {
        Cliente cliente = clienteExistente(5);
        Cuenta cuenta = cuentaExistente(5);
        simularRepositoriosDeActualizacion(cliente, cuenta);

        clienteService.actualizarCliente(actualizacionValida(5));

        assertThat(cliente.getActivo()).isTrue();
        assertThat(cuenta.getEstatus()).isEqualTo(Cuenta.ESTATUS_ACTIVA);
        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void registrarCliente_edadMayorALaMaxima_lanzaValidacionConCampo() {
        ClienteRequest request = solicitudValida();
        request.setFechaNacimiento(LocalDate.now().minusYears(130));

        assertThatThrownBy(() -> clienteService.registrarCliente(request))
                .isInstanceOfSatisfying(NegocioException.class, ex -> {
                    assertThat(ex.getMessage()).contains("no puede ser mayor");
                    assertThat(ex.getCampo()).isEqualTo("fechaNacimiento");
                });
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    void registrarCliente_telefonoAlternativoIgualAlMovil_lanzaValidacionConCampo() {
        ClienteRequest request = solicitudValida();
        request.setTelefonoAlternativo(request.getTelefonoMovil());

        assertThatThrownBy(() -> clienteService.registrarCliente(request))
                .isInstanceOfSatisfying(NegocioException.class, ex -> {
                    assertThat(ex.getMessage()).contains("no puede ser igual");
                    assertThat(ex.getCampo()).isEqualTo("telefonoAlternativo");
                });
        verifyNoInteractions(cuentaRepository);
    }

    @Test
    void registrarCliente_curpDeOtraPersona_lanzaValidacionConCampoCurp() {
        ClienteRequest request = solicitudValida();
        request.setCurp("PEPJ900101HDFRRN01"); // CURP de otra persona (no coincide con Garcia Lopez Juan)

        assertThatThrownBy(() -> clienteService.registrarCliente(request))
                .isInstanceOfSatisfying(NegocioException.class,
                        ex -> assertThat(ex.getCampo()).isEqualTo("curp"));
    }

    @Test
    void registrarCliente_curpDuplicada_indicaElCampoCurp() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.registrarCliente(solicitudValida()))
                .isInstanceOfSatisfying(NegocioException.class,
                        ex -> assertThat(ex.getCampo()).isEqualTo("curp"));
    }
}
