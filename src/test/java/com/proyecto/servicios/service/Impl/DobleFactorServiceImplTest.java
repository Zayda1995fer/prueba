package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Codigo2fa;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.onboarding.Codigo2faRequest;
import com.proyecto.servicios.model.onboarding.Codigo2faResponse;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.Codigo2faRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.service.CifradoService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DobleFactorServiceImplTest {

    @Mock
    private Codigo2faRepository codigoRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CifradoService cifradoService;

    private DobleFactorServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new DobleFactorServiceImpl(codigoRepository, cuentaRepository, clienteRepository, cifradoService);
        ReflectionTestUtils.setField(servicio, "vigenciaMinutos", 5);
        ReflectionTestUtils.setField(servicio, "maxIntentos", 3);
        ReflectionTestUtils.setField(servicio, "devolverCodigo", true);
    }

    private Codigo2faRequest solicitud() {
        Codigo2faRequest r = new Codigo2faRequest();
        r.setNumeroCuenta("1234567890");
        r.setIdUser(5);
        return r;
    }

    private Cuenta cuenta() {
        Cuenta c = new Cuenta();
        c.setId(9);
        c.setClienteId(5);
        c.setNumeroCuenta("1234567890");
        c.setEstatus(Cuenta.ESTATUS_ACTIVA);
        return c;
    }

    private Cliente cliente(boolean activo) {
        Cliente c = new Cliente();
        c.setId(5);
        c.setActivo(activo);
        return c;
    }

    // El "hash" de las pruebas es solo un prefijo: basta para comparar
    private void hashDePrueba() {
        when(cifradoService.hashParaBusqueda(anyString())).thenAnswer(inv -> "H:" + inv.getArgument(0));
    }

    private Codigo2fa guardado(String codigo, LocalDateTime expira, int intentos) {
        Codigo2fa c = new Codigo2fa();
        c.setClienteId(5);
        c.setCuentaId(9);
        c.setCodigoHash("H:" + codigo);
        c.setFechaExpiracion(expira);
        c.setIntentos(intentos);
        c.setUsado(false);
        return c;
    }

    private void hayCodigoVigente(Codigo2fa codigo) {
        when(codigoRepository.findFirstByClienteIdAndCuentaIdAndUsadoFalseOrderByFechaCreacionDesc(5, 9))
                .thenReturn(Optional.of(codigo));
    }

    private void assertError(Runnable accion, CatalogoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(NegocioException.class,
                ex -> assertThat(ex.getError()).isEqualTo(esperado));
    }

    // ---------------- solicitar ----------------

    @Test
    void solicitar_generaCodigoDe6DigitosGuardaSoloElHashYAnulaLosAnteriores() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta()));
        when(clienteRepository.findById(5)).thenReturn(Optional.of(cliente(true)));
        Codigo2fa anterior = guardado("111111", LocalDateTime.now().plusMinutes(1), 0);
        when(codigoRepository.findByClienteIdAndCuentaIdAndUsadoFalse(5, 9)).thenReturn(List.of(anterior));
        hashDePrueba();
        when(codigoRepository.save(any(Codigo2fa.class))).thenAnswer(inv -> inv.getArgument(0));

        Codigo2faResponse response = servicio.solicitar(solicitud());

        assertThat(response.getCodigo()).matches("^[0-9]{6}$");
        assertThat(response.getVigenciaMinutos()).isEqualTo(5);
        assertThat(response.getFechaExpiracion()).isAfter(LocalDateTime.now().plusMinutes(4));
        assertThat(anterior.getUsado()).isTrue();

        ArgumentCaptor<Codigo2fa> capturado = ArgumentCaptor.forClass(Codigo2fa.class);
        verify(codigoRepository, org.mockito.Mockito.atLeastOnce()).save(capturado.capture());
        Codigo2fa nuevo = capturado.getAllValues().get(capturado.getAllValues().size() - 1);
        assertThat(nuevo.getCodigoHash()).isEqualTo("H:" + response.getCodigo());
        assertThat(nuevo.getCodigoHash()).isNotEqualTo(response.getCodigo());
    }

    @Test
    void solicitar_conDevolverCodigoApagado_noRegresaElCodigo() {
        ReflectionTestUtils.setField(servicio, "devolverCodigo", false);
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta()));
        when(clienteRepository.findById(5)).thenReturn(Optional.of(cliente(true)));
        when(codigoRepository.findByClienteIdAndCuentaIdAndUsadoFalse(5, 9)).thenReturn(List.of());
        hashDePrueba();
        when(codigoRepository.save(any(Codigo2fa.class))).thenAnswer(inv -> inv.getArgument(0));

        assertThat(servicio.solicitar(solicitud()).getCodigo()).isNull();
    }

    @Test
    void solicitar_cuentaDeOtroCliente_seRechaza() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta()));
        Codigo2faRequest request = solicitud();
        request.setIdUser(99);

        assertError(() -> servicio.solicitar(request), CatalogoError.CUENTA_NO_PERTENECE_AL_CLIENTE);
        verify(codigoRepository, never()).save(any(Codigo2fa.class));
    }

    @Test
    void solicitar_clienteInactivo_seRechaza() {
        when(cuentaRepository.findByNumeroCuenta("1234567890")).thenReturn(Optional.of(cuenta()));
        when(clienteRepository.findById(5)).thenReturn(Optional.of(cliente(false)));

        assertError(() -> servicio.solicitar(solicitud()), CatalogoError.PAGO_CLIENTE_INACTIVO);
    }

    // ---------------- validar ----------------

    @Test
    void validar_codigoCorrecto_loConsume() {
        Codigo2fa vigente = guardado("654321", LocalDateTime.now().plusMinutes(3), 0);
        hayCodigoVigente(vigente);
        hashDePrueba();

        servicio.validar(5, 9, "654321");

        assertThat(vigente.getUsado()).isTrue();
        verify(codigoRepository).save(vigente);
    }

    @Test
    void validar_sinCodigoSolicitado() {
        when(codigoRepository.findFirstByClienteIdAndCuentaIdAndUsadoFalseOrderByFechaCreacionDesc(5, 9))
                .thenReturn(Optional.empty());
        assertError(() -> servicio.validar(5, 9, "123456"), CatalogoError.DOBLE_FACTOR_NO_SOLICITADO);
    }

    @Test
    void validar_codigoExpirado_loAnula() {
        Codigo2fa vencido = guardado("654321", LocalDateTime.now().minusMinutes(1), 0);
        hayCodigoVigente(vencido);

        assertError(() -> servicio.validar(5, 9, "654321"), CatalogoError.DOBLE_FACTOR_EXPIRADO);
        assertThat(vencido.getUsado()).isTrue();
    }

    @Test
    void validar_codigoIncorrecto_sumaUnIntentoYNoLoConsume() {
        Codigo2fa vigente = guardado("654321", LocalDateTime.now().plusMinutes(3), 0);
        hayCodigoVigente(vigente);
        hashDePrueba();

        assertError(() -> servicio.validar(5, 9, "000000"), CatalogoError.DOBLE_FACTOR_INVALIDO);
        assertThat(vigente.getIntentos()).isEqualTo(1);
        assertThat(vigente.getUsado()).isFalse();
        verify(codigoRepository).save(vigente);
    }

    @Test
    void validar_conLosIntentosAgotados_bloqueaAunqueElCodigoSeaCorrecto() {
        Codigo2fa bloqueado = guardado("654321", LocalDateTime.now().plusMinutes(3), 3);
        hayCodigoVigente(bloqueado);

        assertError(() -> servicio.validar(5, 9, "654321"), CatalogoError.DOBLE_FACTOR_BLOQUEADO);
        assertThat(bloqueado.getUsado()).isFalse();
    }

    // El conteo de intentos debe sobrevivir al rechazo del pago (si no, el código se podría adivinar sin límite)
    @Test
    void validar_usaTransaccionPropiaQueNoSeRevierteConElErrorDeNegocio() throws Exception {
        Transactional anotacion = DobleFactorServiceImpl.class
                .getMethod("validar", Integer.class, Integer.class, String.class)
                .getAnnotation(Transactional.class);

        assertThat(anotacion.propagation()).isEqualTo(Propagation.REQUIRES_NEW);
        assertThat(anotacion.noRollbackFor()).contains(NegocioException.class);
    }
}
