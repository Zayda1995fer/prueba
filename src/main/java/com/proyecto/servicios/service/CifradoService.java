package com.proyecto.servicios.service;

public interface CifradoService {

    String cifrar(String textoPlano);

    String descifrar(String textoCifrado);

    String hashParaBusqueda(String texto);
}