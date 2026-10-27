package com.proyecto.servicios.client;

import com.proyecto.servicios.model.gestopago.GestoPagoConsultaRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxRequest;
import com.proyecto.servicios.model.gestopago.GestoPagoTxResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

/**
 * Cliente de la operación de pago y de su consulta. Feign NO reintenta por defecto
 * (Retryer.NEVER_RETRY): un pago jamás se reenvía de forma automática.
 */
@FeignClient(name = "gestoPagoTx", url = "${gestopago.auth.url}")
public interface GestoPagoTxClient {

    @PostMapping(value = "${gestopago.tx.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    GestoPagoTxResponse enviarTx(@RequestHeader("Authorization") String autorizacion,
                                 @RequestBody GestoPagoTxRequest request);

    @PostMapping(value = "${gestopago.consulta.path}", consumes = MediaType.APPLICATION_JSON_VALUE)
    GestoPagoTxResponse consultarTx(@RequestHeader("Authorization") String autorizacion,
                                    @RequestBody GestoPagoConsultaRequest request);
}
