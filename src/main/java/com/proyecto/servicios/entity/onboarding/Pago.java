package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Un pago a GestoPago. El saldo se descuenta al crearlo (PENDIENTE) y solo
 * se devuelve si el proveedor confirma que NO lo aplicó. Si no se sabe qué
 * pasó (timeout, error 5xx) queda POR_CONCILIAR con el saldo retenido.
 */
@Entity
@Table(name = "pagos")
@Getter
@Setter
public class Pago {

    public static final String ESTATUS_PENDIENTE = "PENDIENTE";
    public static final String ESTATUS_EXITOSO = "EXITOSO";
    public static final String ESTATUS_RECHAZADO = "RECHAZADO";
    public static final String ESTATUS_POR_CONCILIAR = "POR_CONCILIAR";
    public static final String ESTATUS_REVISION_MANUAL = "REVISION_MANUAL";

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    /** Llave de idempotencia que manda el cliente: la misma solicitud nunca cobra dos veces. */
    @Column(name = "id_solicitud", nullable = false, updatable = false, unique = true)
    private String idSolicitud;

    /** Referencia propia que viaja al proveedor y sirve para consultar la operación al conciliar. */
    @Column(name = "referencia_proveedor", nullable = false, updatable = false, unique = true)
    private String referenciaProveedor;

    @Column(name = "huella_solicitud", nullable = false, updatable = false)
    private String huellaSolicitud;

    @Column(name = "cuenta_id", nullable = false, updatable = false)
    private Integer cuentaId;

    @Column(name = "cliente_id", nullable = false, updatable = false)
    private Integer clienteId;

    @Column(name = "id_producto", nullable = false, updatable = false)
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false, updatable = false)
    private Integer idServicio;

    @Column(name = "monto", nullable = false, updatable = false)
    private BigDecimal monto;

    @Column(name = "destino", nullable = false, updatable = false)
    private String destino;

    @Column(name = "estatus", nullable = false)
    private String estatus;

    @Column(name = "folio_transaccion")
    private String folioTransaccion;

    @Column(name = "codigo_respuesta")
    private String codigoRespuesta;

    @Column(name = "intentos_conciliacion", nullable = false)
    private Integer intentosConciliacion = 0;

    @Column(name = "saldo_reintegrado", nullable = false)
    private Boolean saldoReintegrado = false;

    @Column(name = "fecha_creacion", nullable = false, updatable = false)
    private LocalDateTime fechaCreacion;

    @Column(name = "fecha_actualizacion", nullable = false)
    private LocalDateTime fechaActualizacion;

    @PrePersist
    void alCrear() {
        fechaCreacion = LocalDateTime.now();
        fechaActualizacion = fechaCreacion;
    }

    @PreUpdate
    void alActualizar() {
        fechaActualizacion = LocalDateTime.now();
    }

    /** true mientras el resultado final con el proveedor no se conoce. */
    public boolean enCurso() {
        return ESTATUS_PENDIENTE.equals(estatus) || ESTATUS_POR_CONCILIAR.equals(estatus);
    }
}
