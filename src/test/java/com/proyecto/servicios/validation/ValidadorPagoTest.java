package com.proyecto.servicios.validation;

import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.onboarding.PagoRequest;
import com.proyecto.servicios.repositorys.onboarding.ProductoPagoRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ValidadorPagoTest {

    @Mock
    private ProductoPagoRepository repository;

    private ValidadorPago validador;

    @BeforeEach
    void setUp() {
        validador = new ValidadorPago(repository);
    }

    private ProductoPago producto(int id, int servicio, String destino, String min, String max, String lista,
                                  boolean activo) {
        ProductoPago p = new ProductoPago();
        p.setIdProducto(id);
        p.setIdServicio(servicio);
        p.setNombre("Producto " + id);
        p.setTipoDestino(destino);
        p.setMontoMinimo(new BigDecimal(min));
        p.setMontoMaximo(new BigDecimal(max));
        p.setMontosPermitidos(lista);
        p.setActivo(activo);
        return p;
    }

    private void luzExiste() {
        when(repository.findById(1)).thenReturn(Optional.of(
                producto(1, 10, ProductoPago.DESTINO_REFERENCIA, "10.00", "10000.00", null, true)));
    }

    private void recargaExiste() {
        when(repository.findById(2)).thenReturn(Optional.of(
                producto(2, 20, ProductoPago.DESTINO_TELEFONO, "10.00", "500.00", "10,20,50,100", true)));
    }

    private PagoRequest luz(String monto) {
        PagoRequest r = new PagoRequest();
        r.setIdServicio(10);
        r.setIdProducto(1);
        r.setMonto(new BigDecimal(monto));
        r.setReferencia("ABC12345");
        return r;
    }

    private PagoRequest recarga(String monto) {
        PagoRequest r = new PagoRequest();
        r.setIdServicio(20);
        r.setIdProducto(2);
        r.setMonto(new BigDecimal(monto));
        r.setTelefono(5512345678L);
        return r;
    }

    private void assertError(Runnable accion, CatalogoError esperado) {
        assertThatThrownBy(accion::run).isInstanceOfSatisfying(NegocioException.class,
                ex -> assertThat(ex.getError()).isEqualTo(esperado));
    }

    @Test
    void pagoDeLuzValido_regresaElProducto() {
        luzExiste();
        assertThat(validador.validar(luz("350.00")).getIdProducto()).isEqualTo(1);
    }

    @Test
    void recargaConMontoDeLaLista_esValida() {
        recargaExiste();
        assertThat(validador.validar(recarga("50")).getIdProducto()).isEqualTo(2);
    }

    @Test
    void productoInexistente_seRechaza() {
        when(repository.findById(1)).thenReturn(Optional.empty());
        assertError(() -> validador.validar(luz("350.00")), CatalogoError.PRODUCTO_PAGO_NO_ENCONTRADO);
    }

    @Test
    void productoInactivo_seTrataComoInexistente() {
        when(repository.findById(1)).thenReturn(Optional.of(
                producto(1, 10, ProductoPago.DESTINO_REFERENCIA, "10", "100", null, false)));
        assertError(() -> validador.validar(luz("50")), CatalogoError.PRODUCTO_PAGO_NO_ENCONTRADO);
    }

    @Test
    void servicioQueNoCorrespondeAlProducto_seRechaza() {
        luzExiste();
        PagoRequest request = luz("350.00");
        request.setIdServicio(99);
        assertError(() -> validador.validar(request), CatalogoError.SERVICIO_NO_COINCIDE);
    }

    @Test
    void luzConTelefono_seRechaza() {
        luzExiste();
        PagoRequest request = luz("350.00");
        request.setReferencia(null);
        request.setTelefono(5512345678L);
        assertError(() -> validador.validar(request), CatalogoError.PAGO_NO_ACEPTA_TELEFONO);
    }

    @Test
    void luzConAmbosDestinos_seRechaza() {
        luzExiste();
        PagoRequest request = luz("350.00");
        request.setTelefono(5512345678L);
        assertError(() -> validador.validar(request), CatalogoError.PAGO_NO_ACEPTA_TELEFONO);
    }

    @Test
    void luzSinReferencia_seRechaza() {
        luzExiste();
        PagoRequest request = luz("350.00");
        request.setReferencia(null);
        assertError(() -> validador.validar(request), CatalogoError.PAGO_REQUIERE_REFERENCIA);
    }

    @Test
    void recargaConReferencia_seRechaza() {
        recargaExiste();
        PagoRequest request = recarga("50");
        request.setReferencia("ABC12345");
        assertError(() -> validador.validar(request), CatalogoError.PAGO_NO_ACEPTA_REFERENCIA);
    }

    @Test
    void recargaSinTelefono_seRechaza() {
        recargaExiste();
        PagoRequest request = recarga("50");
        request.setTelefono(null);
        assertError(() -> validador.validar(request), CatalogoError.PAGO_REQUIERE_TELEFONO);
    }

    @Test
    void montoMenorAlMinimo_seRechaza() {
        luzExiste();
        assertError(() -> validador.validar(luz("5.00")), CatalogoError.MONTO_FUERA_DE_RANGO);
    }

    @Test
    void montoMayorAlMaximo_seRechaza() {
        luzExiste();
        assertError(() -> validador.validar(luz("10000.01")), CatalogoError.MONTO_FUERA_DE_RANGO);
    }

    @Test
    void montoConMasDeDosDecimales_seRechaza() {
        luzExiste();
        assertError(() -> validador.validar(luz("350.001")), CatalogoError.MONTO_NO_PERMITIDO);
    }

    @Test
    void montoConCerosDeSobra_noEsManipulacion() {
        luzExiste();
        assertThat(validador.validar(luz("350.100"))).isNotNull();
    }

    @Test
    void montoFueraDeLaListaCerrada_seRechaza() {
        recargaExiste();
        assertError(() -> validador.validar(recarga("75")), CatalogoError.MONTO_NO_PERMITIDO);
    }

    @Test
    void destinoDe_regresaTelefonoOReferencia() {
        assertThat(ValidadorPago.destinoDe(recarga("50"))).isEqualTo("5512345678");
        assertThat(ValidadorPago.destinoDe(luz("50"))).isEqualTo("ABC12345");
    }

    @Test
    void enmascarar_dejaSoloLosUltimosCuatro() {
        assertThat(ValidadorPago.enmascarar("ABC12345")).isEqualTo("****2345");
        assertThat(ValidadorPago.enmascarar("123")).isEqualTo("****");
        assertThat(ValidadorPago.enmascarar(null)).isEqualTo("****");
    }
}
