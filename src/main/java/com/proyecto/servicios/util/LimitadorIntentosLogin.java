package com.proyecto.servicios.util;


import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.exception.CatalogoError;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;

/**
 * Limita los intentos fallidos de inicio de sesión por usuario: tras
 * {@code maxIntentos} fallos seguidos el usuario queda bloqueado
 * {@code duracionBloqueo}; un inicio de sesión correcto reinicia el conteo.
 *
 * Es en memoria (se reinicia al reiniciar la aplicación y no se comparte
 * entre varias instancias). Para varias instancias habría que llevar este
 * conteo a la base de datos o a Redis.
 */
public class LimitadorIntentosLogin {

    private static final int MAX_USUARIOS_EN_MEMORIA = 10_000;

    private final int maxIntentos;
    private final Duration duracionBloqueo;
    private final Clock reloj;
    private final Map<String, Registro> registros = new HashMap<>();

    public LimitadorIntentosLogin(int maxIntentos, Duration duracionBloqueo) {
        this(maxIntentos, duracionBloqueo, Clock.systemDefaultZone());
    }

    public LimitadorIntentosLogin(int maxIntentos, Duration duracionBloqueo, Clock reloj) {
        this.maxIntentos = maxIntentos;
        this.duracionBloqueo = duracionBloqueo;
        this.reloj = reloj;
    }

    /** Lanza NegocioException (DEMASIADOS_INTENTOS) si el usuario está bloqueado en este momento. */
    public synchronized void verificarNoBloqueado(String usuario) {
        Registro registro = registros.get(clave(usuario));
        if (registro == null || registro.bloqueadoHasta == null) {
            return;
        }
        Instant ahora = reloj.instant();
        if (ahora.isBefore(registro.bloqueadoHasta)) {
            long minutos = Math.max(1, Duration.between(ahora, registro.bloqueadoHasta).toMinutes() + 1);
            throw new NegocioException(CatalogoError.DEMASIADOS_INTENTOS, minutos);
        }
        registros.remove(clave(usuario)); // el bloqueo ya venció
    }

    public synchronized void registrarFallo(String usuario) {
        if (registros.size() >= MAX_USUARIOS_EN_MEMORIA) {
            limpiarVencidos();
            if (registros.size() >= MAX_USUARIOS_EN_MEMORIA) {
                registros.clear(); // protección contra crecimiento sin límite
            }
        }
        Registro registro = registros.computeIfAbsent(clave(usuario), k -> new Registro());
        registro.fallos++;
        if (registro.fallos >= maxIntentos) {
            registro.bloqueadoHasta = reloj.instant().plus(duracionBloqueo);
            registro.fallos = 0;
        }
    }

    public synchronized void registrarExito(String usuario) {
        registros.remove(clave(usuario));
    }

    private void limpiarVencidos() {
        Instant ahora = reloj.instant();
        registros.values().removeIf(r -> r.bloqueadoHasta != null && !ahora.isBefore(r.bloqueadoHasta));
    }

    private String clave(String usuario) {
        return usuario == null ? "" : usuario.toLowerCase();
    }

    private static final class Registro {
        private int fallos;
        private Instant bloqueadoHasta;
    }
}
