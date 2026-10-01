package com.proyecto.servicios.service;

import com.proyecto.servicios.model.gestopago.GestoPagoProductDTO;

import java.util.List;

// Contrato del servicio: solo dice QUÉ se puede hacer, no CÓMO se hace.
public interface ProductService {

    List<GestoPagoProductDTO> obtenerListaProductos();
}
