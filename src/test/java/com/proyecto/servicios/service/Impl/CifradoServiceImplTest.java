package com.proyecto.servicios.service.Impl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CifradoServiceImplTest {

    private static final String LLAVE_PRUEBA = "ySyJiU10dr71D5cBm5n4EUZ+qwJtJj/eW7foLQp63Z0=";

    private CifradoServiceImpl cifradoService;

    @BeforeEach
    void setUp() {
        cifradoService = new CifradoServiceImpl(LLAVE_PRUEBA);
    }

    @Test
    void cifrarYDescifrar_regresaElTextoOriginal() {
        String original = "0.8734512";

        String cifrado = cifradoService.cifrar(original);
        String descifrado = cifradoService.descifrar(cifrado);

        assertThat(cifrado).isNotEqualTo(original);
        assertThat(descifrado).isEqualTo(original);
    }

    @Test
    void cifrarElMismoTextoDosVeces_daResultadosDistintos() {
        String original = "token-de-ejemplo";

        String cifrado1 = cifradoService.cifrar(original);
        String cifrado2 = cifradoService.cifrar(original);

        assertThat(cifrado1).isNotEqualTo(cifrado2);
    }

    @Test
    void hashParaBusqueda_esSiempreElMismoParaElMismoTexto() {
        String token = "token-de-ejemplo";

        String hash1 = cifradoService.hashParaBusqueda(token);
        String hash2 = cifradoService.hashParaBusqueda(token);

        assertThat(hash1).isEqualTo(hash2);
    }

    @Test
    void hashParaBusqueda_esDistintoParaTextosDistintos() {
        String hashA = cifradoService.hashParaBusqueda("token-A");
        String hashB = cifradoService.hashParaBusqueda("token-B");

        assertThat(hashA).isNotEqualTo(hashB);
    }
}