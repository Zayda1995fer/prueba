package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;

// Este cliente se conecta al servicio externo que nos da la lista de productos.
// Usa la misma dirección (url) que ya usamos para autenticarnos en GestoPago.
@FeignClient(name = "gestoPagoProduct", url = "${gestopago.auth.url}")
public interface GestoPagoProductClient {

    // Pedimos la lista de productos. El token se envía como parámetro
    // para que quien llama a este método decida cuál token usar.
    @GetMapping("${gestopago.products.path}")
    GestoPagoProductListResponse getProductList(@RequestHeader("Authorization") String tokenAutorizacion);
}
