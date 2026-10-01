package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoProductClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.model.gestopago.GestoPagoProductDTO;
import com.proyecto.servicios.model.gestopago.GestoPagoProductListResponse;
import com.proyecto.servicios.service.GestoPagoTokenService;
import feign.FeignException;
import feign.Request;
import feign.RequestTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

// Pruebas simples de la capa de servicio: revisamos el caso normal
// y un par de casos donde algo sale mal.
@ExtendWith(MockitoExtension.class)
class ProductServiceImplTest {

    @Mock
    private GestoPagoProductClient productClient;

    @Mock
    private GestoPagoTokenService tokenService;

    private ProductServiceImpl productService;

    @BeforeEach
    void setUp() {
        productService = new ProductServiceImpl(productClient, tokenService);
    }

    @Test
    void siTodoSaleBien_regresaLaListaDeProductos() {
        // Preparamos un token "guardado" de mentira
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("abc123");
        when(tokenService.obtenerTokenActivo(any(), anyString())).thenReturn(Optional.of(token));

        // Preparamos una respuesta de mentira del servicio externo
        GestoPagoProductDTO producto = new GestoPagoProductDTO();
        producto.setCodigo("P001");
        producto.setNombre("Producto de prueba");

        GestoPagoProductListResponse respuesta = new GestoPagoProductListResponse();
        respuesta.setData(List.of(producto));
        when(productClient.getProductList(anyString())).thenReturn(respuesta);

        // Ejecutamos y revisamos que la respuesta sea la esperada
        List<GestoPagoProductDTO> resultado = productService.obtenerListaProductos();

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getCodigo()).isEqualTo("P001");
    }

    @Test
    void siNoHayTokenGuardado_lanzaError() {
        when(tokenService.obtenerTokenActivo(any(), anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.obtenerListaProductos())
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("token");
    }

    @Test
    void siElServicioExternoFalla_lanzaError() {
        GestoPagoToken token = new GestoPagoToken();
        token.setToken("abc123");
        when(tokenService.obtenerTokenActivo(any(), anyString())).thenReturn(Optional.of(token));

        Request peticionFalsa = Request.create(Request.HttpMethod.GET, "/sistema/service/getProductList.do",
                Collections.emptyMap(), null, new RequestTemplate());

        when(productClient.getProductList(anyString()))
                .thenThrow(new FeignException.InternalServerError("Error", peticionFalsa, null, Map.of()));

        assertThatThrownBy(() -> productService.obtenerListaProductos())
                .isInstanceOf(RuntimeException.class);
    }
}
