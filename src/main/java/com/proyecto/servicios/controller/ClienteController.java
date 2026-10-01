package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

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
    public ResponseEntity<ClienteResponse> consultarPorId(@PathVariable Integer id) {
        return ResponseEntity.ok(clienteService.consultarPorId(id));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<ClienteResponse> actualizar(@PathVariable Integer id,
                                                      @Valid @RequestBody ClienteUpdateRequest request) {
        return ResponseEntity.ok(clienteService.actualizarCliente(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> darDeBaja(@PathVariable Integer id) {
        clienteService.darDeBajaCliente(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/curp/{curp}")
    public ResponseEntity<ClienteResponse> consultarPorCurp(@PathVariable String curp) {
        return ResponseEntity.ok(clienteService.consultarPorCurp(curp));
    }

    @GetMapping("/rfc/{rfc}")
    public ResponseEntity<ClienteResponse> consultarPorRfc(@PathVariable String rfc) {
        return ResponseEntity.ok(clienteService.consultarPorRfc(rfc));
    }

    @GetMapping("/correo/{correo}")
    public ResponseEntity<ClienteResponse> consultarPorCorreo(@PathVariable String correo) {
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