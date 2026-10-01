package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.GestoPagoProductDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import com.proyecto.servicios.service.ProductService;
import feign.FeignException;
import feign.RetryableException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@Slf4j
public class ProductServiceImpl implements ProductService {

    // Cliente que llama al servicio externo de productos
    private final GestoPagoProductClient productClient;

    // Servicio que ya sabe guardar y renovar el token de GestoPago
    private final GestoPagoTokenService tokenService;

    // Estos dos datos identifican nuestra cuenta ante GestoPago.
    // Vienen del archivo application.properties, no están escritos aquí.
    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    public ProductServiceImpl(GestoPagoProductClient productClient, GestoPagoTokenService tokenService) {
        this.productClient = productClient;
        this.tokenService = tokenService;
    }

    @Override
    public List<GestoPagoProductDTO> obtenerListaProductos() {

        // 1) Avisamos en el log que empezamos la consulta
        log.info("Iniciando consulta del catálogo de productos");

        // 2) Buscamos el token que ya tenemos guardado (no lo escribimos a mano en ningún lado)
        Optional<GestoPagoToken> tokenGuardado =
                tokenService.obtenerTokenActivo(idDistribuidor, codigoDispositivo);

        if (tokenGuardado.isEmpty()) {
            log.error("No hay un token disponible para consultar productos");
            throw new RuntimeException("No fue posible autenticarse: no hay un token disponible");
        }

        // Armamos el encabezado de autorización que pide el servicio externo
        String tokenParaEnviar = "Bearer " + tokenGuardado.get().getToken();

        // 3) Llamamos al servicio externo y controlamos los distintos problemas que puedan salir
        try {
            GestoPagoProductListResponse respuesta = productClient.getProductList(tokenParaEnviar);

            log.info("Consulta de productos finalizada correctamente");

            // Si el servicio no mandó productos, regresamos una lista vacía en vez de nada (null)
            return respuesta.getData() != null ? respuesta.getData() : new ArrayList<>();

        } catch (FeignException.Unauthorized e) {
            // El servicio externo no aceptó nuestro token
            log.error("El servicio de productos rechazó nuestras credenciales");
            throw new RuntimeException("No fue posible autenticarse con el servicio de productos");

        } catch (RetryableException e) {
            // El servicio externo tardó demasiado en contestar
            log.error("El servicio de productos no respondió a tiempo");
            throw new RuntimeException("El servicio de productos tardó demasiado en responder");

        } catch (FeignException e) {
            // El servicio contestó, pero con un error (por ejemplo, 500)
            log.error("El servicio de productos respondió con un error, código: {}", e.status());
            throw new RuntimeException("El servicio de productos respondió con un error");

        } catch (Exception e) {
            // Cualquier otro problema inesperado al comunicarnos
            log.error("Ocurrió un problema inesperado al consultar productos");
            throw new RuntimeException("No fue posible comunicarse con el servicio de productos");
        }
    }
}
