package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoTxClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.model.gestopago.GestoPagoConsultaRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago;
import com.proyecto.servicios.service.GestoPagoGateway;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;

/**
 * Reglas para interpretar al proveedor (ver ResultadoGestoPago):
 *  - Éxito solo si el código de respuesta es el de éxito configurado.
 *  - Rechazo (se devuelve el saldo) solo cuando el proveedor lo dice de forma definitiva:
 *    código de error en una respuesta 2xx, o un 4xx que indica que ni siquiera la procesó.
 *  - Todo lo demás (timeout, corte de red, 5xx, 408, 409, respuesta ilegible) es
 *    INDETERMINADO: puede que el proveedor sí haya aplicado el pago.
 * El token Bearer se toma del almacén (se renueva con GestoPagoTokenService) y NUNCA se registra en logs.
 */
@Slf4j
@Service
public class GestoPagoGatewayImpl implements GestoPagoGateway {

    private static final DateTimeFormatter HORA_LOCAL = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final GestoPagoTxClient client;
    private final GestoPagoTokenService tokenService;

    @Value("${gestopago.auth.url}")
    private String url;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    /** Identificador de usuario del sendTx; por defecto el distribuidor. */
    @Value("${gestopago.tx.id-user:${gestopago.auth.id-distribuidor}}")
    private Integer idUser;

    @Value("${gestopago.tx.upc:}")
    private String upc;

    @Value("${gestopago.tx.zona-horaria:America/Mexico_City}")
    private String zonaHoraria;

    @Value("${gestopago.tx.codigo-exito:0}")
    private String codigoExito;

    /** Códigos de respuesta que significan "aún en proceso" (no es éxito ni rechazo). */
    @Value("${gestopago.tx.codigos-pendientes:}")
    private String codigosPendientes;

    /** Códigos de la consulta que significan "el proveedor no aplicó esa operación". */
    @Value("${gestopago.consulta.codigos-no-aplicada:}")
    private String codigosNoAplicada;

    public GestoPagoGatewayImpl(GestoPagoTxClient client, GestoPagoTokenService tokenService) {
        this.client = client;
        this.tokenService = tokenService;
    }

    /** El token y los datos del pago solo viajan por canal cifrado. */
    @PostConstruct
    void validarTransporteSeguro() {
        boolean local = url.startsWith("http://localhost") || url.startsWith("http://127.0.0.1");
        if (!url.startsWith("https://") && !local) {
            throw new IllegalStateException("La URL de GestoPago debe usar https");
        }
    }

    // ------------------------------------------------------------------
    // Envío
    // ------------------------------------------------------------------

    @Override
    public ResultadoGestoPago enviarPago(Pago pago, ProductoPago producto) {
        GestoPagoTxRequest request = new GestoPagoTxRequest(
                producto.getIdProducto(), idUser, pago.getDestino(),
                LocalDateTime.now(ZoneId.of(zonaHoraria)).format(HORA_LOCAL),
                pago.getMonto().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                upc, pago.getReferenciaProveedor());

        log.info("Enviando pago a GestoPago, pago={}, producto={}", pago.getId(), producto.getIdProducto());
        return llamar(pago, autorizacion -> client.enviarTx(autorizacion, request), this::interpretarEnvio, false);
    }

    @Override
    public ResultadoGestoPago consultarPago(Pago pago) {
        GestoPagoConsultaRequest request = new GestoPagoConsultaRequest(pago.getReferenciaProveedor());
        log.info("Consultando pago en GestoPago, pago={}", pago.getId());
        // En la consulta solo el código del proveedor puede decidir un rechazo; un error de red o un 4xx NO lo prueba
        return llamar(pago, autorizacion -> client.consultarTx(autorizacion, request), this::interpretarConsulta, true);
    }

    // ------------------------------------------------------------------
    // Llamada y manejo de errores de red
    // ------------------------------------------------------------------

    private ResultadoGestoPago llamar(Pago pago, Function<String, GestoPagoTxResponse> llamada,
                                      Function<GestoPagoTxResponse, ResultadoGestoPago> interprete,
                                      boolean esConsulta) {
        Optional<String> token = obtenerToken(false);
        if (token.isEmpty()) {
            // No salió nada hacia el proveedor: es seguro devolver el saldo
            log.error("Sin token de GestoPago, no se pudo atender el pago={}", pago.getId());
            return esConsulta ? ResultadoGestoPago.indeterminado("SIN_TOKEN")
                    : ResultadoGestoPago.rechazado("SIN_TOKEN", CatalogoError.GESTOPAGO_SIN_TOKEN);
        }
        try {
            return interprete.apply(llamada.apply("Bearer " + token.get()));
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            // El proveedor rechazó el token antes de procesar nada: se renueva y se intenta una vez más
            log.warn("GestoPago rechazó el token (HTTP {}), se renueva y se reintenta una vez", e.status());
            return reintentarConTokenNuevo(pago, llamada, interprete, esConsulta);
        } catch (FeignException e) {
            return clasificar(pago, e, esConsulta);
        } catch (RuntimeException e) {
            log.error("Resultado desconocido del proveedor, pago={}, error={}", pago.getId(), e.getClass().getSimpleName());
            return ResultadoGestoPago.indeterminado("ERROR_" + e.getClass().getSimpleName());
        }
    }

    private ResultadoGestoPago reintentarConTokenNuevo(Pago pago, Function<String, GestoPagoTxResponse> llamada,
                                                       Function<GestoPagoTxResponse, ResultadoGestoPago> interprete,
                                                       boolean esConsulta) {
        Optional<String> token = obtenerToken(true);
        if (token.isEmpty()) {
            return esConsulta ? ResultadoGestoPago.indeterminado("SIN_TOKEN")
                    : ResultadoGestoPago.rechazado("SIN_TOKEN", CatalogoError.GESTOPAGO_SIN_TOKEN);
        }
        try {
            return interprete.apply(llamada.apply("Bearer " + token.get()));
        } catch (FeignException.Unauthorized | FeignException.Forbidden e) {
            log.error("GestoPago siguió rechazando el token, pago={}", pago.getId());
            return esConsulta ? ResultadoGestoPago.indeterminado("HTTP_" + e.status())
                    : ResultadoGestoPago.rechazado("HTTP_" + e.status(), CatalogoError.GESTOPAGO_SIN_TOKEN);
        } catch (FeignException e) {
            return clasificar(pago, e, esConsulta);
        } catch (RuntimeException e) {
            return ResultadoGestoPago.indeterminado("ERROR_" + e.getClass().getSimpleName());
        }
    }

    /**
     * Un 4xx (salvo 408 y 409) significa que el proveedor rechazó la petición sin procesarla.
     * Cualquier otra cosa —5xx, timeout (status -1), 408, 409, error al leer la respuesta— deja la
     * duda de si el pago se aplicó, así que NO se devuelve el saldo.
     */
    private ResultadoGestoPago clasificar(Pago pago, FeignException e, boolean esConsulta) {
        int status = e.status();
        String codigo = status > 0 ? "HTTP_" + status : "SIN_RESPUESTA";
        if (esConsulta) {
            log.error("La consulta del pago={} no fue concluyente, estado={}", pago.getId(), codigo);
            return ResultadoGestoPago.indeterminado(codigo);
        }
        if (status == 429) {
            log.warn("GestoPago limitó la solicitud (429), pago={}", pago.getId());
            return ResultadoGestoPago.rechazado(codigo, CatalogoError.GESTOPAGO_NO_DISPONIBLE);
        }
        if (status >= 400 && status < 500 && status != 408 && status != 409) {
            log.error("GestoPago rechazó la solicitud, pago={}, HTTP {}", pago.getId(), status);
            return ResultadoGestoPago.rechazado(codigo, CatalogoError.PAGO_RECHAZADO);
        }
        log.error("Resultado incierto con GestoPago, pago={}, estado={}", pago.getId(), codigo);
        return ResultadoGestoPago.indeterminado(codigo);
    }

    // ------------------------------------------------------------------
    // Interpretación de las respuestas
    // ------------------------------------------------------------------

    private ResultadoGestoPago interpretarEnvio(GestoPagoTxResponse respuesta) {
        if (respuesta == null || respuesta.getCodigo() == null || respuesta.getCodigo().isBlank()) {
            return ResultadoGestoPago.indeterminado("RESPUESTA_VACIA");
        }
        String codigo = respuesta.getCodigo().trim();
        if (codigo.equals(codigoExito)) {
            return ResultadoGestoPago.exitoso(codigo, respuesta.getFolioTransaccion());
        }
        if (enLista(codigosPendientes, codigo)) {
            return ResultadoGestoPago.indeterminado(codigo);
        }
        return ResultadoGestoPago.rechazado(codigo, CatalogoError.PAGO_RECHAZADO);
    }

    private ResultadoGestoPago interpretarConsulta(GestoPagoTxResponse respuesta) {
        if (respuesta == null || respuesta.getCodigo() == null || respuesta.getCodigo().isBlank()) {
            return ResultadoGestoPago.indeterminado("RESPUESTA_VACIA");
        }
        String codigo = respuesta.getCodigo().trim();
        if (codigo.equals(codigoExito)) {
            return ResultadoGestoPago.exitoso(codigo, respuesta.getFolioTransaccion());
        }
        if (enLista(codigosNoAplicada, codigo)) {
            return ResultadoGestoPago.rechazado(codigo, CatalogoError.PAGO_RECHAZADO);
        }
        // Código que no sabemos interpretar: no se arriesga dinero, se deja por conciliar
        return ResultadoGestoPago.indeterminado(codigo);
    }

    private boolean enLista(String lista, String codigo) {
        if (lista == null || lista.isBlank()) {
            return false;
        }
        List<String> codigos = Arrays.stream(lista.split(",")).map(String::trim).toList();
        return codigos.contains(codigo);
    }

    // ------------------------------------------------------------------
    // Token
    // ------------------------------------------------------------------

    private Optional<String> obtenerToken(boolean renovarAntes) {
        if (renovarAntes) {
            tokenService.renovarToken();
        }
        Optional<GestoPagoToken> token = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
        if (token.isEmpty() && !renovarAntes) {
            tokenService.renovarToken();
            token = tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);
        }
        return token.map(GestoPagoToken::getToken).filter(t -> !t.isBlank());
    }
}
