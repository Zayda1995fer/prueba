package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mapstruct.factory.Mappers;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Base64;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/** El token Bearer de GestoPago se guarda cifrado y solo se descifra al usarlo. */
@ExtendWith(MockitoExtension.class)
class GestoPagoTokenServiceImplTest {

    private static final String TOKEN = "eyJhbGciOiJIUzI1NiJ9.token-secreto";

    @Mock
    private GestoPagoAuthClient authClient;
    @Mock
    private GestoPagoTokenRepository tokenRepository;

    private final CifradoServiceImpl cifrado =
            new CifradoServiceImpl(Base64.getEncoder().encodeToString(new byte[32]));
    private GestoPagoTokenServiceImpl servicio;

    @BeforeEach
    void setUp() {
        servicio = new GestoPagoTokenServiceImpl(authClient, tokenRepository,
                Mappers.getMapper(GestoPagoTokenMapper.class), cifrado);
        ReflectionTestUtils.setField(servicio, "idDistribuidor", 83);
        ReflectionTestUtils.setField(servicio, "codigoDispositivo", "GPS83-TPV-17");
        ReflectionTestUtils.setField(servicio, "password", "clave");
    }

    @Test
    void renovarToken_loGuardaCifradoNuncaEnTextoPlano() {
        GestoPagoAuthResponse respuesta = new GestoPagoAuthResponse();
        respuesta.setToken(TOKEN);
        when(authClient.authenticate(83, "GPS83-TPV-17", "clave")).thenReturn(respuesta);
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.empty());

        servicio.renovarToken();

        ArgumentCaptor<GestoPagoToken> guardado = ArgumentCaptor.forClass(GestoPagoToken.class);
        verify(tokenRepository).save(guardado.capture());
        assertThat(guardado.getValue().getToken()).isNotEqualTo(TOKEN).doesNotContain("token-secreto");
        assertThat(cifrado.descifrar(guardado.getValue().getToken())).isEqualTo(TOKEN);
    }

    @Test
    void obtenerTokenActivo_devuelveUnaCopiaDescifradaYNoModificaLaEntidad() {
        GestoPagoToken enBd = new GestoPagoToken();
        enBd.setActivo(true);
        enBd.setToken(cifrado.cifrar(TOKEN));
        String cifradoEnBd = enBd.getToken();
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(enBd));

        Optional<GestoPagoToken> token = servicio.obtenerTokenActivo(83, "GPS83-TPV-17");

        assertThat(token).map(GestoPagoToken::getToken).contains(TOKEN);
        assertThat(enBd.getToken()).isEqualTo(cifradoEnBd);
    }

    @Test
    void obtenerTokenActivo_tokenQueNoSePuedeDescifrar_seTrataComoInexistente() {
        GestoPagoToken enBd = new GestoPagoToken();
        enBd.setActivo(true);
        enBd.setToken("token-guardado-en-texto-plano");
        when(tokenRepository.findByIdDistribuidorAndCodigoDispositivo(83, "GPS83-TPV-17")).thenReturn(Optional.of(enBd));

        assertThat(servicio.obtenerTokenActivo(83, "GPS83-TPV-17")).isEmpty();
    }

    @Test
    void elTokenNoApareceEnElToStringDeLaRespuesta() {
        GestoPagoAuthResponse respuesta = new GestoPagoAuthResponse();
        respuesta.setToken(TOKEN);
        assertThat(respuesta.toString()).doesNotContain("token-secreto");
    }
}
