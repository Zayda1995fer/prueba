package com.proyecto.servicios.service.Impl;

import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.exception.CatalogoError;
import com.proyecto.servicios.service.CifradoService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.Cipher;
import javax.crypto.Mac;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Cifra y descifra con AES-GCM. La llave NUNCA vive en el código: se lee
 * desde application.properties (app.seguridad.aes-key), igual que ya se
 * hacía con la contraseña de GestoPago (gestopago.auth.password).
 *
 * Por qué AES y no, por ejemplo, BCrypt (que ya se usa para las
 * contraseñas): BCrypt es de UN SOLO SENTIDO — sirve para comparar
 * ("¿esta contraseña coincide con el hash?") pero nunca puede regresar
 * al valor original. El dato biométrico y el token de sesión sí
 * necesitan poder "volver" a su valor original para usarse, así que
 * hace falta cifrado reversible (AES), no un hash.
 */
@Service
@Slf4j
public class CifradoServiceImpl implements CifradoService {

    private static final String ALGORITMO = "AES/GCM/NoPadding";
    private static final int TAMANO_TAG_GCM_BITS = 128;
    private static final int TAMANO_IV_BYTES = 12;

    private final SecretKeySpec llave;
    private final SecureRandom random = new SecureRandom();

    public CifradoServiceImpl(@Value("${app.seguridad.aes-key}") String llaveBase64) {
        byte[] llaveBytes = Base64.getDecoder().decode(llaveBase64);
        this.llave = new SecretKeySpec(llaveBytes, "AES");
    }

    @Override
    public String cifrar(String textoPlano) {
        if (textoPlano == null) {
            return null;
        }
        try {
            byte[] iv = new byte[TAMANO_IV_BYTES];
            random.nextBytes(iv);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.ENCRYPT_MODE, llave, new GCMParameterSpec(TAMANO_TAG_GCM_BITS, iv));
            byte[] textoCifrado = cipher.doFinal(textoPlano.getBytes());

            // Se guarda el IV junto con el texto cifrado (el IV no es
            // secreto, solo debe ser distinto cada vez); así al descifrar
            // sabemos con qué IV se cifró este dato en particular.
            byte[] resultado = new byte[iv.length + textoCifrado.length];
            System.arraycopy(iv, 0, resultado, 0, iv.length);
            System.arraycopy(textoCifrado, 0, resultado, iv.length, textoCifrado.length);

            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            log.error("Error al cifrar dato sensible: {}", e.getClass().getSimpleName());
            throw new NegocioException(CatalogoError.DATO_SENSIBLE_PROTEGER);
        }
    }

    @Override
    public String descifrar(String textoCifradoBase64) {
        if (textoCifradoBase64 == null) {
            return null;
        }
        try {
            byte[] datos = Base64.getDecoder().decode(textoCifradoBase64);

            byte[] iv = new byte[TAMANO_IV_BYTES];
            System.arraycopy(datos, 0, iv, 0, TAMANO_IV_BYTES);
            byte[] textoCifrado = new byte[datos.length - TAMANO_IV_BYTES];
            System.arraycopy(datos, TAMANO_IV_BYTES, textoCifrado, 0, textoCifrado.length);

            Cipher cipher = Cipher.getInstance(ALGORITMO);
            cipher.init(Cipher.DECRYPT_MODE, llave, new GCMParameterSpec(TAMANO_TAG_GCM_BITS, iv));
            byte[] textoPlano = cipher.doFinal(textoCifrado);

            return new String(textoPlano);
        } catch (Exception e) {
            // Nunca se loguea el dato ni la llave, solo que algo falló
            log.error("Error al descifrar dato sensible: {}", e.getClass().getSimpleName());
            throw new NegocioException(CatalogoError.DATO_SENSIBLE_LEER);
        }
    }

    @Override
    public String hashParaBusqueda(String texto) {
        if (texto == null) {
            return null;
        }
        try {
            // HMAC-SHA256: usa la misma llave secreta, pero a diferencia de
            // cifrar(), el resultado SIEMPRE es el mismo para el mismo
            // texto (no lleva IV aleatorio) y jamás se puede revertir.
            // Por eso sirve para búsquedas y no para guardar el valor real.
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(llave);
            byte[] resultado = mac.doFinal(texto.getBytes());
            return Base64.getEncoder().encodeToString(resultado);
        } catch (Exception e) {
            log.error("Error al calcular hash de búsqueda: {}", e.getClass().getSimpleName());
            throw new NegocioException(CatalogoError.DATO_SENSIBLE_PROCESAR);
        }
    }
}
