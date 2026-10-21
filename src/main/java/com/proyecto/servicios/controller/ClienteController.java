package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.RespuestaExitosa;
import com.proyecto.servicios.model.onboarding.ClienteConsultaRequest;
import com.proyecto.servicios.model.onboarding.ClienteRequest;
import com.proyecto.servicios.model.onboarding.ClienteResponse;
import com.proyecto.servicios.model.onboarding.ClienteUpdateRequest;
import com.proyecto.servicios.service.ClienteService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * CRUD de clientes. Reglas de este controlador:
 *  - toda la información (datos, id y criterios de búsqueda) llega en un
 *    Request DTO con @RequestBody + @Valid; no hay @PathVariable ni @RequestParam,
 *  - no hay validaciones ni try-catch aquí: las validaciones viven en los DTO y
 *    los errores los procesa GlobalExceptionHandler (@RestControllerAdvice),
 *  - nunca se reciben ni se devuelven entidades JPA, solo DTO,
 *  - solo hay 3 endpoints: crear (POST), consultar con filtros (POST /consultar) y actualizar
 *    (PUT, que también da de baja o reactiva con "activo"),
 *  - todas las respuestas exitosas usan RespuestaExitosa {success, codigo, message, data}.
 */
@RestController
@RequestMapping(value = "/clientes", produces = MediaType.APPLICATION_JSON_VALUE)
public class ClienteController {

    private final ClienteService clienteService;

    public ClienteController(ClienteService clienteService) {
        this.clienteService = clienteService;
    }

    // Alta -> 201 Created
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaExitosa<ClienteResponse>> registrar(@Valid @RequestBody ClienteRequest request) {
        ClienteResponse cliente = clienteService.registrarCliente(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(RespuestaExitosa.de(HttpStatus.CREATED, "Cliente registrado correctamente", cliente));
    }

    // Único endpoint de consulta: filtros opcionales y combinables en el body -> 200 OK
    @PostMapping(value = "/consultar", consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaExitosa<List<ClienteResponse>>> consultar(
            @Valid @RequestBody ClienteConsultaRequest criterios) {
        List<ClienteResponse> clientes = clienteService.consultarClientes(criterios);
        return ResponseEntity.ok(RespuestaExitosa.de(HttpStatus.OK, "Consulta realizada correctamente", clientes));
    }

    // Actualización: el id del cliente viaja en el body -> 200 OK.
    // La baja/reactivación del cliente se hace aquí mismo con el campo opcional "activo".
    @PutMapping(consumes = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<RespuestaExitosa<ClienteResponse>> actualizar(
            @Valid @RequestBody ClienteUpdateRequest request) {
        ClienteResponse cliente = clienteService.actualizarCliente(request);
        return ResponseEntity.ok(RespuestaExitosa.de(HttpStatus.OK, "Cliente actualizado correctamente", cliente));
    }
}
