package com.proyecto.servicios.entity.onboarding;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Catálogo local de lo que se puede pagar. Define qué destino exige el pago
 * (teléfono o referencia) y qué montos se aceptan, para que un monto o un
 * producto manipulado en la petición nunca llegue al proveedor.
 */
@Entity
@Table(name = "catalogo_productos_pago")
@Getter
@Setter
public class ProductoPago {

    public static final String DESTINO_TELEFONO = "TELEFONO";
    public static final String DESTINO_REFERENCIA = "REFERENCIA";

    @Id
    @Column(name = "id_producto")
    private Integer idProducto;

    @Column(name = "id_servicio", nullable = false)
    private Integer idServicio;

    @Column(name = "nombre", nullable = false)
    private String nombre;

    @Column(name = "tipo_destino", nullable = false)
    private String tipoDestino;

    @Column(name = "monto_minimo", nullable = false)
    private BigDecimal montoMinimo;

    @Column(name = "monto_maximo", nullable = false)
    private BigDecimal montoMaximo;

    /** Lista cerrada de montos separados por coma (p. ej. "50,100,200"); null si acepta cualquiera del rango. */
    @Column(name = "montos_permitidos")
    private String montosPermitidos;

    @Column(name = "activo", nullable = false)
    private Boolean activo = true;
}
