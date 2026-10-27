package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.entity.onboarding.Pago;
import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.model.gestopago.ResultadoGestoPago;
import com.proyecto.servicios.repositorys.onboarding.PagoRepository;
import com.proyecto.servicios.service.ConciliacionService;
import com.proyecto.servicios.service.GestoPagoGateway;
import com.proyecto.servicios.service.PagoTransaccionService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
public class ConciliacionServiceImpl implements ConciliacionService {

    private final PagoRepository pagoRepository;
    private final GestoPagoGateway gateway;
    private final PagoTransaccionService transacciones;

    @Value("${app.pagos.conciliacion.activa:true}")
    private boolean activa;

    /** Tiempo mínimo desde el último movimiento antes de consultar (debe ser mayor al timeout de Feign). */
    @Value("${app.pagos.conciliacion.espera-segundos:120}")
    private int esperaSegundos;

    @Value("${app.pagos.conciliacion.max-intentos:10}")
    private int maxIntentos;

    @Value("${app.pagos.conciliacion.tamano-lote:20}")
    private int tamanoLote;

    public ConciliacionServiceImpl(PagoRepository pagoRepository, GestoPagoGateway gateway,
                                   PagoTransaccionService transacciones) {
        this.pagoRepository = pagoRepository;
        this.gateway = gateway;
        this.transacciones = transacciones;
    }

    @Scheduled(fixedDelayString = "${app.pagos.conciliacion.intervalo-ms:60000}",
            initialDelayString = "${app.pagos.conciliacion.inicio-ms:30000}")
    public void tareaProgramada() {
        if (activa) {
            conciliarPendientes();
        }
    }

    @Override
    public int conciliarPendientes() {
        List<Pago> pendientes = pagoRepository.buscarPorConciliar(
                List.of(Pago.ESTATUS_PENDIENTE, Pago.ESTATUS_POR_CONCILIAR),
                LocalDateTime.now().minusSeconds(esperaSegundos), maxIntentos, PageRequest.of(0, tamanoLote));

        for (Pago pago : pendientes) {
            try {
                ResultadoGestoPago resultado = gateway.consultarPago(pago);
                transacciones.registrarResultado(pago.getId(), resultado, true);
            } catch (Exception e) {
                // Un pago con problemas no detiene a los demás
                log.error("No fue posible conciliar el pago={} ({})", pago.getId(), e.getClass().getSimpleName());
            }
        }
        if (!pendientes.isEmpty()) {
            log.info("Conciliación: {} pago(s) revisados", pendientes.size());
        }
        return pendientes.size();
    }
}
