package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Cliente;
import com.proyecto.servicios.entity.onboarding.Cuenta;
import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago;
import com.proyecto.servicios.model.onboarding.PagoRequest;
import com.proyecto.servicios.repositorys.onboarding.ClienteRepository;
import com.proyecto.servicios.repositorys.onboarding.CuentaRepository;
import com.proyecto.servicios.repositorys.onboarding.PagoRepository;
import com.proyecto.servicios.repositorys.onboarding.ProductoPagoRepository;
import com.proyecto.servicios.service.DobleFactorService;
import com.proyecto.servicios.service.NotificacionService;
import com.proyecto.servicios.service.PagoTransaccionService.Reserva;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PagoTransaccionServiceImplTest {

    private static final String ID_SOLICITUD = "3f2b8c1e-9a4d-4c7e-8b21-5d6e7f8a9b0c";

    @Mock
    private ClienteRepository clienteRepository;
    @Mock
    private CuentaRepository cuentaRepository;
    @Mock
    private PagoRepository pagoRepository;
    @Mock
    private ProductoPagoRepository productoRepository;
    @Mock
    private DobleFactorService dobleFactorService;
    @Mock
    private NotificacionService notificacionService;

    private PagoTransaccionServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new PagoTransaccionServiceImpl(clienteRepository, cuentaRepository, pagoRepository,
                productoRepository, dobleFactorService, notificacionService);
        ReflectionTestUtils.setField(servicio, "ventanaDuplicadosSegundos", 60);
        ReflectionTestUtils.setField(servicio, "maxIntentosConciliacion", 3);
    }

    // ---------------- datos de prueba ----------------

    private ProductoPago luz() {
        ProductoPago p = new ProductoPago();
        p.setIdProducto(1);
        p.setIdServicio(10);
        p.setNombre("Pago de luz (CFE)");
        p.setTipoDestino(ProductoPago.DESTINO_REFERENCIA);
        return p;
    }

    private PagoRequest request(String monto) {
        PagoRequest r = new PagoRequest();
        r.setIdSolicitud(ID_SOLICITUD);
        r.setNumeroCuenta("1234567890");
        r.setIdUser(5);
        r.setIdServicio(10);
        r.setIdProducto(1);
        r.setMonto(new BigDecimal(monto));
        r.setReferencia("ABC12345");
        r.setCodigo2fa("123456");
        return r;
    }

    private Cuenta cuenta(String saldo) {
        Cuenta c = new Cuenta();
        c.setId(9);
        c.setClienteId(5);
        c.setNumeroCuenta("1234567890");
        c.setSaldo(new BigDecimal(saldo));
        c.setEstatus(Cuenta.ESTATUS_ACTIVA);
        return c;
    }

    private Cliente cliente(boolean activo) {
        Cliente c = new Cliente();
        c.setId(5);
        c.setNombre("Ana");
        c.setCorreoElectronico("ana@correo.com");
        c.setActivo(activo);
        return c;
    }

    private void cuentaExiste(String saldo) {
        when(cuentaRepository.buscarParaPago("1234567890")).thenReturn(Optional.of(cuenta(saldo)));
    }

    private void clienteExiste(boolean activo) {
        when(clienteRepository.findById(5)).thenReturn(Optional.of(cliente(activo)));
    }

    private void assertError(Runnable accion, CatalogoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(NegocioException.class,
                ex -> assertThat(ex.getError()).isEqualTo(esperado));
    }

    private Pago pagoEnCurso(String estatus) {
        Pago p = new Pago();
        p.setId(7);
        p.setIdSolicitud(ID_SOLICITUD);
        p.setCuentaId(9);
        p.setClienteId(5);
        p.setIdProducto(1);
        p.setMonto(new BigDecimal("350.00"));
        p.setEstatus(estatus);
        return p;
    }

    // ---------------- reservar ----------------

    @Test
    void reservar_descuentaElSaldoYRegistraElPagoPendiente() {
        Cuenta cuenta = cuenta("2000.00");
        when(cuentaRepository.buscarParaPago("1234567890")).thenReturn(Optional.of(cuenta));
        clienteExiste(true);
        when(pagoRepository.contarSimilares(any(), any(), any(), any(), anyCollection(), any())).thenReturn(0L);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));

        Reserva reserva = servicio.reservar(request("350.00"), luz());

        assertThat(reserva.repeticion()).isFalse();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");
        Pago pago = reserva.pago();
        assertThat(pago.getEstatus()).isEqualTo(Pago.ESTATUS_PENDIENTE);
        assertThat(pago.getIdSolicitud()).isEqualTo(ID_SOLICITUD);
        assertThat(pago.getDestino()).isEqualTo("ABC12345");
        assertThat(pago.getMonto()).isEqualByComparingTo("350.00");
        assertThat(pago.getReferenciaProveedor()).hasSize(32);
        assertThat(pago.getHuellaSolicitud()).hasSize(64);

        // El 2FA se valida antes de tocar el saldo
        InOrder orden = inOrder(dobleFactorService, cuentaRepository);
        orden.verify(dobleFactorService).validar(5, 9, "123456");
        orden.verify(cuentaRepository).save(cuenta);
    }

    @Test
    void reservar_laMismaSolicitudDevuelveElPagoOriginalSinCobrarOtraVez() {
        Cuenta cuenta = cuenta("2000.00");
        when(cuentaRepository.buscarParaPago("1234567890")).thenReturn(Optional.of(cuenta));
        clienteExiste(true);
        when(pagoRepository.contarSimilares(any(), any(), any(), any(), anyCollection(), any())).thenReturn(0L);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        Pago original = servicio.reservar(request("350.00"), luz()).pago();

        when(pagoRepository.findByIdSolicitud(ID_SOLICITUD)).thenReturn(Optional.of(original));
        Reserva segunda = servicio.reservar(request("350.00"), luz());

        assertThat(segunda.repeticion()).isTrue();
        assertThat(segunda.pago()).isSameAs(original);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");   // un solo descuento
        verify(cuentaRepository, times(1)).save(any(Cuenta.class));
        verify(dobleFactorService, times(1)).validar(any(), any(), any());
    }

    @Test
    void reservar_mismaSolicitudConOtroMonto_esConflicto() {
        Cuenta cuenta = cuenta("2000.00");
        when(cuentaRepository.buscarParaPago("1234567890")).thenReturn(Optional.of(cuenta));
        clienteExiste(true);
        when(pagoRepository.contarSimilares(any(), any(), any(), any(), anyCollection(), any())).thenReturn(0L);
        when(pagoRepository.save(any(Pago.class))).thenAnswer(inv -> inv.getArgument(0));
        Pago original = servicio.reservar(request("350.00"), luz()).pago();

        when(pagoRepository.findByIdSolicitud(ID_SOLICITUD)).thenReturn(Optional.of(original));

        assertError(() -> servicio.reservar(request("999.00"), luz()), CatalogoError.IDEMPOTENCIA_CONFLICTO);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");
    }

    @Test
    void reservar_solicitudDeOtroCliente_esConflicto() {
        cuentaExiste("2000.00");
        Pago ajeno = pagoEnCurso(Pago.ESTATUS_PENDIENTE);
        ajeno.setClienteId(77);
        ajeno.setHuellaSolicitud("x");
        when(pagoRepository.findByIdSolicitud(ID_SOLICITUD)).thenReturn(Optional.of(ajeno));

        assertError(() -> servicio.reservar(request("350.00"), luz()), CatalogoError.IDEMPOTENCIA_CONFLICTO);
    }

    @Test
    void reservar_cuentaInexistente() {
        when(cuentaRepository.buscarParaPago("1234567890")).thenReturn(Optional.empty());
        assertError(() -> servicio.reservar(request("350.00"), luz()), CatalogoError.CUENTA_NO_ENCONTRADA);
    }

    @Test
    void reservar_cuentaDeOtroCliente() {
        cuentaExiste("2000.00");
        PagoRequest otro = request("350.00");
        otro.setIdUser(99);
        assertError(() -> servicio.reservar(otro, luz()), CatalogoError.CUENTA_NO_PERTENECE_AL_CLIENTE);
        verify(dobleFactorService, never()).validar(any(), any(), any());
    }

    @Test
    void reservar_clienteInactivo() {
        cuentaExiste("2000.00");
        clienteExiste(false);
        assertError(() -> servicio.reservar(request("350.00"), luz()), CatalogoError.PAGO_CLIENTE_INACTIVO);
    }

    @Test
    void reservar_cuentaInactiva() {
        Cuenta inactiva = cuenta("2000.00");
        inactiva.setEstatus(Cuenta.ESTATUS_INACTIVA);
        when(cuentaRepository.buscarParaPago("1234567890")).thenReturn(Optional.of(inactiva));
        clienteExiste(true);
        assertError(() -> servicio.reservar(request("350.00"), luz()), CatalogoError.PAGO_CLIENTE_INACTIVO);
    }

    @Test
    void reservar_saldoInsuficiente_noPidePruebaNiDescuenta() {
        Cuenta cuenta = cuenta("100.00");
        when(cuentaRepository.buscarParaPago("1234567890")).thenReturn(Optional.of(cuenta));
        clienteExiste(true);

        assertError(() -> servicio.reservar(request("350.00"), luz()), CatalogoError.SALDO_INSUFICIENTE);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("100.00");
        verify(dobleFactorService, never()).validar(any(), any(), any());
    }

    @Test
    void reservar_pagoIgualReciente_seRechazaComoDuplicadoAntesDelDobleFactor() {
        cuentaExiste("2000.00");
        clienteExiste(true);
        when(pagoRepository.contarSimilares(any(), any(), any(), any(), anyCollection(), any())).thenReturn(1L);

        assertError(() -> servicio.reservar(request("350.00"), luz()), CatalogoError.PAGO_DUPLICADO);
        verify(dobleFactorService, never()).validar(any(), any(), any());
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    @Test
    void reservar_dobleFactorInvalido_noDescuentaNiRegistra() {
        Cuenta cuenta = cuenta("2000.00");
        when(cuentaRepository.buscarParaPago("1234567890")).thenReturn(Optional.of(cuenta));
        clienteExiste(true);
        when(pagoRepository.contarSimilares(any(), any(), any(), any(), anyCollection(), any())).thenReturn(0L);
        doThrow(new NegocioException(CatalogoError.DOBLE_FACTOR_INVALIDO))
                .when(dobleFactorService).validar(5, 9, "123456");

        assertError(() -> servicio.reservar(request("350.00"), luz()), CatalogoError.DOBLE_FACTOR_INVALIDO);
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("2000.00");
        verify(pagoRepository, never()).save(any(Pago.class));
    }

    // ---------------- registrarResultado ----------------

    @Test
    void registrarResultado_exitoso_confirmaGuardaFolioYAvisaAlCliente() {
        Pago pago = pagoEnCurso(Pago.ESTATUS_PENDIENTE);
        when(pagoRepository.buscarParaActualizar(7)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(pago)).thenReturn(pago);
        clienteExiste(true);
        Cuenta cuenta = cuenta("1650.00");
        when(cuentaRepository.findById(9)).thenReturn(Optional.of(cuenta));
        when(productoRepository.findById(1)).thenReturn(Optional.of(luz()));

        Pago resultado = servicio.registrarResultado(7, ResultadoGestoPago.exitoso("0", "FOL-1"), false);

        assertThat(resultado.getEstatus()).isEqualTo(Pago.ESTATUS_EXITOSO);
        assertThat(resultado.getFolioTransaccion()).isEqualTo("FOL-1");
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("1650.00");   // el saldo no se vuelve a tocar
        verify(notificacionService).notificarPago(eq("ana@correo.com"), eq("Ana"), eq("1234567890"),
                eq("Pago de luz (CFE)"), eq(new BigDecimal("350.00")), eq(new BigDecimal("1650.00")),
                eq("FOL-1"), any());
    }

    @Test
    void registrarResultado_rechazado_devuelveElSaldoUnaSolaVez() {
        Pago pago = pagoEnCurso(Pago.ESTATUS_PENDIENTE);
        when(pagoRepository.buscarParaActualizar(7)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(pago)).thenReturn(pago);
        Cuenta cuenta = cuenta("1650.00");
        when(cuentaRepository.buscarPorIdParaActualizar(9)).thenReturn(Optional.of(cuenta));

        Pago resultado = servicio.registrarResultado(7,
                ResultadoGestoPago.rechazado("5", CatalogoError.PAGO_RECHAZADO), false);

        assertThat(resultado.getEstatus()).isEqualTo(Pago.ESTATUS_RECHAZADO);
        assertThat(resultado.getCodigoRespuesta()).isEqualTo("5");
        assertThat(resultado.getSaldoReintegrado()).isTrue();
        assertThat(cuenta.getSaldo()).isEqualByComparingTo("2000.00");
        verify(notificacionService, never()).notificarPago(any(), any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void registrarResultado_rechazoConSaldoYaReintegrado_noDevuelveDeNuevo() {
        Pago pago = pagoEnCurso(Pago.ESTATUS_POR_CONCILIAR);
        pago.setSaldoReintegrado(true);
        when(pagoRepository.buscarParaActualizar(7)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(pago)).thenReturn(pago);

        servicio.registrarResultado(7, ResultadoGestoPago.rechazado("10", CatalogoError.PAGO_RECHAZADO), true);

        verify(cuentaRepository, never()).buscarPorIdParaActualizar(any());
        verify(cuentaRepository, never()).save(any(Cuenta.class));
    }

    @Test
    void registrarResultado_indeterminado_dejaElSaldoRetenidoPorConciliar() {
        Pago pago = pagoEnCurso(Pago.ESTATUS_PENDIENTE);
        when(pagoRepository.buscarParaActualizar(7)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(pago)).thenReturn(pago);

        Pago resultado = servicio.registrarResultado(7, ResultadoGestoPago.indeterminado("SIN_RESPUESTA"), false);

        assertThat(resultado.getEstatus()).isEqualTo(Pago.ESTATUS_POR_CONCILIAR);
        assertThat(resultado.getIntentosConciliacion()).isZero();
        verify(cuentaRepository, never()).save(any(Cuenta.class));
        verify(cuentaRepository, never()).buscarPorIdParaActualizar(any());
    }

    @Test
    void registrarResultado_conciliacionSinRespuesta_cuentaIntentosYAlLlegarAlMaximoPasaARevisionManual() {
        Pago pago = pagoEnCurso(Pago.ESTATUS_POR_CONCILIAR);
        when(pagoRepository.buscarParaActualizar(7)).thenReturn(Optional.of(pago));
        when(pagoRepository.save(pago)).thenReturn(pago);

        servicio.registrarResultado(7, ResultadoGestoPago.indeterminado("HTTP_500"), true);
        assertThat(pago.getEstatus()).isEqualTo(Pago.ESTATUS_POR_CONCILIAR);
        assertThat(pago.getIntentosConciliacion()).isEqualTo(1);

        servicio.registrarResultado(7, ResultadoGestoPago.indeterminado("HTTP_500"), true);
        servicio.registrarResultado(7, ResultadoGestoPago.indeterminado("HTTP_500"), true);

        assertThat(pago.getEstatus()).isEqualTo(Pago.ESTATUS_REVISION_MANUAL);
        verify(cuentaRepository, never()).save(any(Cuenta.class));   // el saldo sigue retenido
    }

    @Test
    void registrarResultado_pagoYaResuelto_noSeModifica() {
        Pago pago = pagoEnCurso(Pago.ESTATUS_EXITOSO);
        pago.setFolioTransaccion("FOL-1");
        when(pagoRepository.buscarParaActualizar(7)).thenReturn(Optional.of(pago));

        Pago resultado = servicio.registrarResultado(7,
                ResultadoGestoPago.rechazado("5", CatalogoError.PAGO_RECHAZADO), true);

        assertThat(resultado.getEstatus()).isEqualTo(Pago.ESTATUS_EXITOSO);
        verify(pagoRepository, never()).save(any(Pago.class));
        verify(cuentaRepository, never()).buscarPorIdParaActualizar(any());
    }

    @Test
    void registrarResultado_pagoInexistente() {
        when(pagoRepository.buscarParaActualizar(7)).thenReturn(Optional.empty());
        assertError(() -> servicio.registrarResultado(7, ResultadoGestoPago.indeterminado("x"), false),
                CatalogoError.PAGO_NO_ENCONTRADO);
    }
}
