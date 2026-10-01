package com.proyecto.servicios.controller;

import com.proyecto.servicios.model.gestopago.GestoPagoProductDTO;
import com.proyecto.servicios.service.ProductService;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

// Este controller solo expone la dirección web que van a usar para pedir los productos.
// Toda la lógica real vive en ProductService, aquí no se hacen cálculos ni validaciones.
@RestController
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    // Al entrar a /productos, se devuelve la lista de productos del servicio externo
    @GetMapping(value = "/productos", produces = MediaType.APPLICATION_JSON_VALUE)
    public ResponseEntity<List<GestoPagoProductDTO>> obtenerProductos() {
        return ResponseEntity.ok(productService.obtenerListaProductos());
    }
}
