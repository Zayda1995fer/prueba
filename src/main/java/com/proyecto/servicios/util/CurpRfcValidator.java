package com.proyecto.servicios.util;


import com.proyecto.servicios.exception.NegocioException;
import com.proyecto.servicios.exception.CatalogoError;
import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Verifica que la CURP y el RFC capturados realmente se correspondan con
 * el resto de los datos del cliente (nombre, apellidos, fecha de
 * nacimiento y sexo), y no solo que tengan el formato correcto.
 *
 * El formato (longitud, letras/dígitos en el lugar correcto) ya se
 * valida con @Pattern en ClienteRequest/ClienteUpdateRequest. Esta clase
 * valida el CONTENIDO: que las letras y la fecha dentro de la CURP/RFC
 * coincidan con lo que el cliente escribió en los demás campos, usando
 * el mismo algoritmo oficial con el que se construyen esos documentos.
 *
 * Simplificación consciente: la regla oficial de RENAPO para el "nombre"
 * ignora palabras como "MARIA" o "JOSE" cuando van seguidas de un
 * segundo nombre (p. ej. "MARIA GUADALUPE" usa "GUADALUPE"). Ese caso
 * especial no se implementa aquí; se usa siempre la primera letra del
 * campo "nombre" tal cual se capturó, que cubre el caso normal.
 */
public final class CurpRfcValidator {

    private CurpRfcValidator() {
    }

    /**
     * @param nombre           nombre(s) del cliente
     * @param apellidoPaterno  apellido paterno
     * @param apellidoMaterno  apellido materno
     * @param fechaNacimiento  fecha de nacimiento
     * @param sexo             "H" o "M"
     * @param curp             CURP capturada (ya validada en formato)
     * @param rfc              RFC capturado (ya validado en formato)
     * @throws NegocioException si la CURP o el RFC no coinciden con los demás datos
     */
    public static void validar(String nombre, String apellidoPaterno, String apellidoMaterno,
                                 LocalDate fechaNacimiento, String sexo,
                                 String curp, String rfc) {

        String letrasEsperadas =
                primeraLetra(apellidoPaterno)
                        + primeraVocalInterna(apellidoPaterno)
                        + primeraLetra(apellidoMaterno)
                        + primeraLetra(nombre);

        String fechaEsperada = fechaNacimiento.format(DateTimeFormatter.ofPattern("yyMMdd"));

        String curpUpper = curp == null ? "" : curp.toUpperCase();
        String rfcUpper = rfc == null ? "" : rfc.toUpperCase();

        if (curpUpper.length() < 11) {
            throw new NegocioException(CatalogoError.CURP_LONGITUD);
        }

        // --- CURP: letras iniciales ---
        String letrasCurp = curpUpper.substring(0, 4);
        if (!letrasCurp.equals(letrasEsperadas)) {
            throw new NegocioException(CatalogoError.CURP_NOMBRE, letrasEsperadas);
        }

        // --- CURP: fecha de nacimiento ---
        String fechaCurp = curpUpper.substring(4, 10);
        if (!fechaCurp.equals(fechaEsperada)) {
            throw new NegocioException(CatalogoError.CURP_FECHA, fechaEsperada);
        }

        // --- CURP: sexo ---
        char sexoEsperado = sexo == null || sexo.isBlank() ? '?' : Character.toUpperCase(sexo.trim().charAt(0));
        if (sexoEsperado != 'H' && sexoEsperado != 'M') {
            throw new NegocioException(CatalogoError.SEXO_INVALIDO);
        }
        char sexoCurp = curpUpper.charAt(10);
        if (sexoCurp != sexoEsperado) {
            throw new NegocioException(CatalogoError.CURP_SEXO, sexoCurp, sexoEsperado);
        }

        // --- RFC: letras iniciales (mismo algoritmo que la CURP) ---
        if (rfcUpper.length() < 10) {
            throw new NegocioException(CatalogoError.RFC_LONGITUD);
        }
        String letrasRfc = rfcUpper.substring(0, 4);
        if (!letrasRfc.equals(letrasEsperadas)) {
            throw new NegocioException(CatalogoError.RFC_NOMBRE, letrasEsperadas);
        }

        // --- RFC: fecha de nacimiento ---
        String fechaRfc = rfcUpper.substring(4, 10);
        if (!fechaRfc.equals(fechaEsperada)) {
            throw new NegocioException(CatalogoError.RFC_FECHA, fechaEsperada);
        }

        // --- Consistencia cruzada CURP/RFC entre sí ---
        if (!letrasCurp.equals(letrasRfc) || !fechaCurp.equals(fechaRfc)) {
            throw new NegocioException(CatalogoError.CURP_RFC_INCONSISTENTES);
        }
    }

    private static String primeraLetra(String texto) {
        String limpio = quitarAcentosYNoLetras(texto);
        if (limpio.isEmpty()) {
            return "X";
        }
        return String.valueOf(limpio.charAt(0));
    }

    private static String primeraVocalInterna(String texto) {
        String limpio = quitarAcentosYNoLetras(texto);
        for (int i = 1; i < limpio.length(); i++) {
            char c = limpio.charAt(i);
            if (c == 'A' || c == 'E' || c == 'I' || c == 'O' || c == 'U') {
                return String.valueOf(c);
            }
        }
        return "X";
    }

    private static String quitarAcentosYNoLetras(String texto) {
        if (texto == null) {
            return "";
        }
        String sinAcentos = Normalizer.normalize(texto.trim().toUpperCase(), Normalizer.Form.NFD)
                .replaceAll("[\\p{InCombiningDiacriticalMarks}]", "");
        return sinAcentos.replaceAll("[^A-Z]", "");
    }
}
