package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

import static com.proyecto.servicios.validation.ReglasValidacion.CORREO;
import static com.proyecto.servicios.validation.ReglasValidacion.CURP;
import static com.proyecto.servicios.validation.ReglasValidacion.RFC_PERSONA_FISICA;

@RestController
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // --- Endpoints mínimos solicitados ---

    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> registrar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse response = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<List<ClienteResponse>> consultarTodos() {
        return ResponseEntity.ok(clienteService.consultarTodos());
    }

    @GetMapping("/{id}")
    public ResponseEntity<ClienteResponse> consultarPorId(@PathVariable @Positive(message = "El id debe ser un entero positivo") Integer id) {
        return ResponseEntity.ok(clienteService.consultarPorId(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable @Positive(message = "El id debe ser un entero positivo") Integer id,
                                                      @Valid @RequestBody ClienteUpdateRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    // Baja lógica: nunca se borra físicamente el registro
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable @Positive(message = "El id debe ser un entero positivo") Integer id) {
        clienteService.darDeBajaCliente(id);
        return ResponseEntity.noContent().build();
    }

    // --- Consultas solicitadas ---

    @GetMapping("/curp/{curp}")
    public ResponseEntity<ClienteResponse> consultarPorCurp(
            @PathVariable @Pattern(regexp = CURP, flags = Pattern.Flag.CASE_INSENSITIVE,
                    message = "La CURP debe tener 18 caracteres con el formato oficial") String curp) {
        return ResponseEntity.ok(clienteService.consultarPorCurp(curp));
    }

    @GetMapping("/rfc/{rfc}")
    public ResponseEntity<ClienteResponse> consultarPorRfc(
            @PathVariable @Pattern(regexp = RFC_PERSONA_FISICA, flags = Pattern.Flag.CASE_INSENSITIVE,
                    message = "El RFC debe tener 13 caracteres con el formato oficial") String rfc) {
        return ResponseEntity.ok(clienteService.consultarPorRfc(rfc));
    }

    @GetMapping("/correo/{correo}")
    public ResponseEntity<ClienteResponse> consultarPorCorreo(
            @PathVariable @Pattern(regexp = CORREO, flags = Pattern.Flag.CASE_INSENSITIVE,
                    message = "El correo electrónico no tiene un formato válido") String correo) {
        return ResponseEntity.ok(clienteService.consultarPorCorreo(correo));
    }

    @GetMapping("/activos")
    public ResponseEntity<List<ClienteResponse>> consultarActivos() {
        return ResponseEntity.ok(clienteService.consultarActivos());
    }

    @GetMapping("/rango-fechas")
    public ResponseEntity<List<ClienteResponse>> consultarPorRangoDeFechas(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime desde,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime hasta) {
        return ResponseEntity.ok(clienteService.consultarPorRangoDeFechas(desde, hasta));
    }
}