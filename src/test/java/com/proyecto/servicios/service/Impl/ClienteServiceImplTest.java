package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Domicilio;
import com.proyecto.servicios.entity.onboarding.Nacionalidad;
import com.proyecto.servicios.exception.ClienteNoEncontradoException;
import com.proyecto.servicios.exception.ClienteYaRegistradoException;
import com.proyecto.servicios.exception.CurpDuplicadaException;
import com.proyecto.servicios.exception.RfcDuplicadoException;
import com.proyecto.servicios.exception.ValidacionException;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.DomicilioRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.DomicilioRepository;
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
                .isInstanceOf(ValidacionException.class)
                .hasMessageContaining("mayor de edad");

        verifyNoInteractions(cuentaRepository);
    }

    @Test
    void registrarCliente_curpDuplicada_lanzaCurpDuplicadaException() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.registrarCliente(solicitudValida()))
                .isInstanceOf(CurpDuplicadaException.class);
    }

    @Test
    void registrarCliente_rfcDuplicado_lanzaRfcDuplicadoException() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.registrarCliente(solicitudValida()))
                .isInstanceOf(RfcDuplicadoException.class);
    }

    @Test
    void registrarCliente_correoDuplicado_lanzaClienteYaRegistradoException() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(false);
        when(clienteRepository.existsByRfc(anyString())).thenReturn(false);
        when(clienteRepository.existsByCorreoElectronico(anyString())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.registrarCliente(solicitudValida()))
                .isInstanceOf(ClienteYaRegistradoException.class);
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
                .isInstanceOf(ValidacionException.class)
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
                .isInstanceOf(ValidacionException.class)
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

    @Test
    void consultarPorId_noRegresaElId() {
        Cliente cliente = new Cliente();
        cliente.setId(7);
        cliente.setNombre("Juan");
        cliente.setNacionalidadId(1);
        cliente.setIngresoMensual(new java.math.BigDecimal("15000.00"));
        Nacionalidad mexicana = new Nacionalidad();
        mexicana.setId(1);
        mexicana.setNombre("Mexicana");
        when(clienteRepository.findById(7)).thenReturn(Optional.of(cliente));
        when(nacionalidadRepository.findById(1)).thenReturn(Optional.of(mexicana));
        when(domicilioRepository.findByClienteId(7)).thenReturn(Optional.empty());
        when(cuentaRepository.findByClienteId(7)).thenReturn(Optional.empty());

        ClienteResponse response = clienteService.consultarPorId(7);

        assertThat(response.getId()).isNull();
    }

    private static NacionalidadRequest nacionalidad(int id, String nombre) {
        NacionalidadRequest n = new NacionalidadRequest();
        n.setId(id);
        n.setNombre(nombre);
        return n;
    }

    @Test
    void consultarPorId_noExiste_lanzaClienteNoEncontradoException() {
        when(clienteRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.consultarPorId(99))
                .isInstanceOf(ClienteNoEncontradoException.class);
    }

    @Test
    void darDeBajaCliente_marcaClienteInactivoYCuentaInactiva() {
        Cliente cliente = new Cliente();
        cliente.setId(5);
        cliente.setActivo(true);

        Cuenta cuenta = new Cuenta();
        cuenta.setId(9);
        cuenta.setClienteId(5);
        cuenta.setEstatus(Cuenta.ESTATUS_ACTIVA);

        when(clienteRepository.findById(5)).thenReturn(Optional.of(cliente));
        when(clienteRepository.save(any(Cliente.class))).thenAnswer(inv -> inv.getArgument(0));
        when(cuentaRepository.findByClienteId(5)).thenReturn(Optional.of(cuenta));
        when(cuentaRepository.save(any(Cuenta.class))).thenAnswer(inv -> inv.getArgument(0));

        clienteService.darDeBajaCliente(5);

        assertThat(cliente.getActivo()).isFalse();
        assertThat(cuenta.getEstatus()).isEqualTo(Cuenta.ESTATUS_INACTIVA);
    }

    @Test
    void registrarCliente_edadMayorALaMaxima_lanzaValidacionConCampo() {
        ClienteRequest request = solicitudValida();
        request.setFechaNacimiento(LocalDate.now().minusYears(130));

        assertThatThrownBy(() -> clienteService.registrarCliente(request))
                .isInstanceOfSatisfying(ValidacionException.class, ex -> {
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
                .isInstanceOfSatisfying(ValidacionException.class, ex -> {
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
                .isInstanceOfSatisfying(ValidacionException.class,
                        ex -> assertThat(ex.getCampo()).isEqualTo("curp"));
    }

    @Test
    void registrarCliente_curpDuplicada_indicaElCampoCurp() {
        when(clienteRepository.existsByCurp(anyString())).thenReturn(true);

        assertThatThrownBy(() -> clienteService.registrarCliente(solicitudValida()))
                .isInstanceOfSatisfying(CurpDuplicadaException.class,
                        ex -> assertThat(ex.getCampo()).isEqualTo("curp"));
    }

    @Test
    void consultarPorCurp_enMinusculas_buscaEnMayusculas() {
        when(clienteRepository.findByCurp("GALJ900101HDFRPN01")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clienteService.consultarPorCurp("galj900101hdfrpn01"))
                .isInstanceOf(ClienteNoEncontradoException.class);

        verify(clienteRepository).findByCurp("GALJ900101HDFRPN01");
    }
}