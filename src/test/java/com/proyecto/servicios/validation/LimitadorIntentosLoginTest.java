package com.proyecto.servicios.validation;

import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.util.LimitadorIntentosLogin;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LimitadorIntentosLoginTest {

    private static class RelojMutable extends Clock {
        Instant ahora = Instant.parse("2026-01-01T12:00:00Z");

        @Override
        public ZoneId getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return ahora;
        }
    }

    private RelojMutable reloj;
    private LimitadorIntentosLogin limitador;

    @BeforeEach
    void setUp() {
        reloj = new RelojMutable();
        limitador = new LimitadorIntentosLogin(5, Duration.ofMinutes(15), reloj);
    }

    private void fallar(String usuario, int veces) {
        for (int i = 0; i < veces; i++) {
            limitador.verificarNoBloqueado(usuario);
            limitador.registrarFallo(usuario);
        }
    }

    @Test
    void cuatroFallos_noBloquean() {
        fallar("juan", 4);
        assertThatCode(() -> limitador.verificarNoBloqueado("juan")).doesNotThrowAnyException();
    }

    @Test
    void quintoFallo_bloqueaAlUsuario() {
        fallar("juan", 5);
        assertThatThrownBy(() -> limitador.verificarNoBloqueado("juan"))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.DEMASIADOS_INTENTOS))
                .hasMessageContaining("Demasiados intentos");
    }

    @Test
    void pasadoElTiempoDeBloqueo_elUsuarioPuedeIntentarOtraVez() {
        fallar("juan", 5);
        reloj.ahora = reloj.ahora.plus(Duration.ofMinutes(16));
        assertThatCode(() -> limitador.verificarNoBloqueado("juan")).doesNotThrowAnyException();
    }

    @Test
    void unInicioCorrecto_reiniciaElConteo() {
        fallar("juan", 4);
        limitador.registrarExito("juan");
        fallar("juan", 4);
        assertThatCode(() -> limitador.verificarNoBloqueado("juan")).doesNotThrowAnyException();
    }

    @Test
    void elBloqueoEsPorUsuarioYNoDistingueMayusculas() {
        fallar("Juan", 5);
        assertThatThrownBy(() -> limitador.verificarNoBloqueado("JUAN"))
                .isInstanceOfSatisfying(NegocioException.class, ex -> assertThat(ex.getError()).isEqualTo(CatalogoError.DEMASIADOS_INTENTOS));
        assertThatCode(() -> limitador.verificarNoBloqueado("maria")).doesNotThrowAnyException();
    }
}
