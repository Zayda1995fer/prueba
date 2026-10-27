package com.proyecto.servicios.validation;

import com.proyecto.servicios.entity.onboarding.ProductoPago;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.model.onboarding.PagoRequest;
import com.proyecto.servicios.repositorys.onboarding.ProductoPagoRepository;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * Reglas de negocio del pago contra el catálogo: el servicio, el producto,
 * el destino que exige (teléfono o referencia) y el monto permitido.
 * Nada de lo que viene en la petición se envía al proveedor sin pasar por aquí.
 */
@Component
public class ValidadorPago {

    private final ProductoPagoRepository productoRepository;

    public ValidadorPago(ProductoPagoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    public ProductoPago validar(PagoRequest request) {
        ProductoPago producto = productoRepository.findById(request.getIdProducto())
                .filter(p -> Boolean.TRUE.equals(p.getActivo()))
                .orElseThrow(() -> new NegocioException(CatalogoError.PRODUCTO_PAGO_NO_ENCONTRADO,
                        request.getIdProducto()));
        if (!producto.getIdServicio().equals(request.getIdServicio())) {
            throw new NegocioException(CatalogoError.SERVICIO_NO_COINCIDE, request.getIdProducto());
        }
        validarDestino(producto, request);
        validarMonto(producto, request.getMonto());
        return producto;
    }

    /** Teléfono o referencia, según lo que pida el producto; el otro debe venir vacío. */
    private void validarDestino(ProductoPago producto, PagoRequest request) {
        boolean pideTelefono = ProductoPago.DESTINO_TELEFONO.equals(producto.getTipoDestino());
        if (pideTelefono) {
            if (request.getReferencia() != null) {
                throw new NegocioException(CatalogoError.PAGO_NO_ACEPTA_REFERENCIA);
            }
            if (request.getTelefono() == null) {
                throw new NegocioException(CatalogoError.PAGO_REQUIERE_TELEFONO);
            }
        } else {
            if (request.getTelefono() != null) {
                throw new NegocioException(CatalogoError.PAGO_NO_ACEPTA_TELEFONO);
            }
            if (request.getReferencia() == null) {
                throw new NegocioException(CatalogoError.PAGO_REQUIERE_REFERENCIA);
            }
        }
    }

    private void validarMonto(ProductoPago producto, BigDecimal monto) {
        // Más de 2 decimales (350.001) es un monto manipulado o mal formado
        if (monto.stripTrailingZeros().scale() > 2) {
            throw new NegocioException(CatalogoError.MONTO_NO_PERMITIDO, descripcionDeMontos(producto));
        }
        if (monto.compareTo(producto.getMontoMinimo()) < 0 || monto.compareTo(producto.getMontoMaximo()) > 0) {
            throw new NegocioException(CatalogoError.MONTO_FUERA_DE_RANGO,
                    producto.getMontoMinimo().toPlainString(), producto.getMontoMaximo().toPlainString());
        }
        List<BigDecimal> permitidos = montosPermitidos(producto);
        if (!permitidos.isEmpty() && permitidos.stream().noneMatch(m -> m.compareTo(monto) == 0)) {
            throw new NegocioException(CatalogoError.MONTO_NO_PERMITIDO, descripcionDeMontos(producto));
        }
    }

    private String descripcionDeMontos(ProductoPago producto) {
        List<BigDecimal> permitidos = montosPermitidos(producto);
        if (permitidos.isEmpty()) {
            return "de " + producto.getMontoMinimo().toPlainString() + " a "
                    + producto.getMontoMaximo().toPlainString() + " con máximo 2 decimales";
        }
        return permitidos.stream().map(BigDecimal::toPlainString).reduce((a, b) -> a + ", " + b).orElse("");
    }

    private List<BigDecimal> montosPermitidos(ProductoPago producto) {
        List<BigDecimal> lista = new ArrayList<>();
        String texto = producto.getMontosPermitidos();
        if (texto == null || texto.isBlank()) {
            return lista;
        }
        for (String parte : texto.split(",")) {
            if (!parte.isBlank()) {
                lista.add(new BigDecimal(parte.trim()));
            }
        }
        return lista;
    }

    /** El destino que viaja al proveedor: el teléfono o la referencia, el que corresponda. */
    public static String destinoDe(PagoRequest request) {
        return request.getTelefono() != null ? String.valueOf(request.getTelefono()) : request.getReferencia();
    }

    /** Enmascara un destino para mostrarlo o registrarlo: solo se ven los últimos 4 caracteres. */
    public static String enmascarar(String destino) {
        if (destino == null || destino.length() <= 4) {
            return "****";
        }
        return "*".repeat(destino.length() - 4) + destino.substring(destino.length() - 4);
    }
}
