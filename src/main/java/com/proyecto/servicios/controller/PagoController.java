package com.proyecto.servicios.controller;

import com.proyecto.servicios.config.SesionInterceptor;
import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.model.RespuestaExitosa;
import com.proyecto.servicios.model.onboarding.Codigo2faRequest;
import com.proyecto.servicios.model.onboarding.Codigo2faResponse;
import com.proyecto.servicios.model.onboarding.PagoConsultaRequest;
import com.proyecto.servicios.model.onboarding.PagoRequest;
import com.proyecto.servicios.model.onboarding.PagoResponse;
import com.proyecto.servicios.service.DobleFactorService;
import com.proyecto.servicios.service.PagoService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Solo recibe, delega y responde: toda la lógica está en los servicios.
 * Todas las rutas exigen sesión (SesionInterceptor); el cliente autenticado llega como atributo.
 */
@RestController
@RequestMapping(value = "/pagos", produces = MediaType.APPLICATION_JSON_VALUE)
public class PagoController {

    private final PagoService pagoService;
    private final DobleFactorService dobleFactorService;

    public PagoController(PagoService pagoService, DobleFactorService dobleFactorService) {
        this.pagoService = pagoService;
        this.dobleFactorService = dobleFactorService;
    }

    /** 201 si el proveedor confirmó el pago; 202 si aún está por confirmarse (el saldo queda retenido). */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaExitosa<PagoResponse>> pagar(@Valid @RequestBody PagoRequest request,
            @RequestAttribute(SesionInterceptor.CLIENTE_AUTENTICADO) Integer clienteAutenticado) {
        PagoResponse response = pagoService.pagar(request, clienteAutenticado);
        if (Pago.ESTATUS_EXITOSO.equals(response.getEstatus())) {
            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(RespuestaExitosa.de(HttpStatus.CREATED, "Pago realizado correctamente", response));
        }
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(RespuestaExitosa.de(HttpStatus.ACCEPTED,
                "El pago está en proceso de confirmación con el proveedor. El monto queda retenido y se confirmará "
                        + "o se devolverá automáticamente. Consulta el estado con /pagos/consultar", response));
    }

    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaExitosa<PagoResponse>> consultar(@Valid @RequestBody PagoConsultaRequest request,
            @RequestAttribute(SesionInterceptor.CLIENTE_AUTENTICADO) Integer clienteAutenticado) {
        return ResponseEntity.ok(RespuestaExitosa.de(HttpStatus.OK, "Consulta realizada correctamente",
                pagoService.consultarPago(request, clienteAutenticado)));
    }

    @PostMapping(value = "/2fa", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaExitosa<Codigo2faResponse>> solicitarCodigo(
            @Valid @RequestBody Codigo2faRequest request,
            @RequestAttribute(SesionInterceptor.CLIENTE_AUTENTICADO) Integer clienteAutenticado) {
        return ResponseEntity.status(HttpStatus.CREATED).body(RespuestaExitosa.de(HttpStatus.CREATED,
                "Código 2FA generado correctamente", dobleFactorService.solicitar(request, clienteAutenticado)));
    }
}
