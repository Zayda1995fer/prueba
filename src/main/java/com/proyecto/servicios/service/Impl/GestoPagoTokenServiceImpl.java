package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.client.GestoPagoAuthClient;
import com.proyecto.servicios.entity.gestopago.GestoPagoToken;
import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.mapper.GestoPagoTokenMapper;
import com.proyecto.servicios.model.gestopago.GestoPagoAuthResponse;
import com.proyecto.servicios.repositorys.gestopago.GestoPagoTokenRepository;
import com.proyecto.servicios.service.CifradoService;
import com.proyecto.servicios.service.GestoPagoTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@Slf4j
public class GestoPagoTokenServiceImpl implements GestoPagoTokenService {

    private final GestoPagoAuthClient gestoPagoAuthClient;
    private final GestoPagoTokenRepository tokenRepository;
    private final GestoPagoTokenMapper tokenMapper;
    private final CifradoService cifradoService;

    @Value("${gestopago.auth.id-distribuidor}")
    private Integer idDistribuidor;

    @Value("${gestopago.auth.codigo-dispositivo}")
    private String codigoDispositivo;

    @Value("${gestopago.auth.password}")
    private String password;

    public GestoPagoTokenServiceImpl(GestoPagoAuthClient gestoPagoAuthClient,
                                     GestoPagoTokenRepository tokenRepository,
                                     GestoPagoTokenMapper tokenMapper,
                                     CifradoService cifradoService) {
        this.gestoPagoAuthClient = gestoPagoAuthClient;
        this.tokenRepository = tokenRepository;
        this.tokenMapper = tokenMapper;
        this.cifradoService = cifradoService;
    }

    @Override
    @Scheduled(fixedRateString = "${gestopago.auth.refresh-rate-ms:3600000}", initialDelay = 0)
    public void renovarToken() {
        log.info("Renovando token GestoPago para distribuidor={}", idDistribuidor);
        try {
            GestoPagoAuthResponse response = gestoPagoAuthClient.authenticate(
                    idDistribuidor, codigoDispositivo, password);

            if (response == null || response.getToken() == null) {
                log.error("La respuesta de GestoPago no contiene token");
                return;
            }

            GestoPagoToken tokenEntity = tokenRepository
                    .findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                    .map(existing -> {
                        tokenMapper.updateEntity(response, existing);
                        return existing;
                    })
                    .orElseGet(() -> {
                        GestoPagoToken nuevo = tokenMapper.toEntity(response);
                        nuevo.setIdDistribuidor(idDistribuidor);
                        nuevo.setCodigoDispositivo(codigoDispositivo);
                        nuevo.setActivo(true);
                        return nuevo;
                    });

            // El token Bearer se guarda cifrado (AES-GCM): una copia de la BD no basta para usarlo
            tokenEntity.setToken(cifradoService.cifrar(response.getToken()));
            tokenRepository.save(tokenEntity);
            log.info("Token GestoPago renovado correctamente");

        } catch (Exception e) {
            // Solo el tipo de error: el mensaje de Feign incluye la URL de la petición (con la contraseña)
            log.error("Error al renovar token GestoPago ({})", e.getClass().getSimpleName());
        }
    }

    /**
     * Devuelve una COPIA con el token ya descifrado (la entidad administrada se queda cifrada).
     * Si no se puede descifrar (llave rotada o token guardado antes del cifrado) se trata
     * como inexistente para que quien llama solicite uno nuevo.
     */
    @Override
    public Optional<GestoPagoToken> obtenerTokenActivo(Integer idDistribuidor, String codigoDispositivo) {
        return tokenRepository.findByIdDistribuidorAndCodigoDispositivo(idDistribuidor, codigoDispositivo)
                .filter(t -> Boolean.TRUE.equals(t.getActivo()))
                .flatMap(this::copiaDescifrada);
    }

    private Optional<GestoPagoToken> copiaDescifrada(GestoPagoToken guardado) {
        try {
            GestoPagoToken copia = new GestoPagoToken();
            copia.setId(guardado.getId());
            copia.setIdDistribuidor(guardado.getIdDistribuidor());
            copia.setCodigoDispositivo(guardado.getCodigoDispositivo());
            copia.setTokenType(guardado.getTokenType());
            copia.setExpiresIn(guardado.getExpiresIn());
            copia.setActivo(guardado.getActivo());
            copia.setToken(cifradoService.descifrar(guardado.getToken()));
            return Optional.of(copia);
        } catch (NegocioException e) {
            log.warn("El token GestoPago guardado no se pudo descifrar; se solicitará uno nuevo");
            return Optional.empty();
        }
    }
}
