package com.proyecto.servicios.util;

import com.proyecto.servicios.exception.ValidacionException;

import java.text.Normalizer;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * Verifica que la CURP y el RFC capturados realmente se correspondan con
 * el resto de los datos del cliente (nombre, apellidos, fecha de
 * nacimiento y sexo)
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
     * @throws ValidacionException si la CURP o el RFC no coinciden con los demás datos
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
            throw new ValidacionException("La CURP no tiene la longitud suficiente para validarse");
        }

        // --- CURP: letras iniciales ---
        String letrasCurp = curpUpper.substring(0, 4);
        if (!letrasCurp.equals(letrasEsperadas)) {
            throw new ValidacionException(
                    "La CURP no coincide con el nombre y apellidos capturados "
                            + "(se esperaba que empezara con \"" + letrasEsperadas
                            + "\", de acuerdo al apellido paterno, apellido materno y nombre)");
        }

        // --- CURP: fecha de nacimiento ---
        String fechaCurp = curpUpper.substring(4, 10);
        if (!fechaCurp.equals(fechaEsperada)) {
            throw new ValidacionException(
                    "La CURP no coincide con la fecha de nacimiento capturada "
                            + "(se esperaba la fecha " + fechaEsperada + " en formato AAMMDD)");
        }

        // --- CURP: sexo ---
        char sexoEsperado = sexo == null || sexo.isBlank() ? '?' : Character.toUpperCase(sexo.trim().charAt(0));
        if (sexoEsperado != 'H' && sexoEsperado != 'M') {
            throw new ValidacionException("El sexo debe capturarse como \"H\" o \"M\" para poder validar la CURP");
        }
        char sexoCurp = curpUpper.charAt(10);
        if (sexoCurp != sexoEsperado) {
            throw new ValidacionException(
                    "La CURP no coincide con el sexo capturado (la CURP indica \"" + sexoCurp
                            + "\" y se capturó \"" + sexoEsperado + "\")");
        }

        // --- RFC: letras iniciales (mismo algoritmo que la CURP) ---
        if (rfcUpper.length() < 10) {
            throw new ValidacionException("El RFC no tiene la longitud suficiente para validarse");
        }
        String letrasRfc = rfcUpper.substring(0, 4);
        if (!letrasRfc.equals(letrasEsperadas)) {
            throw new ValidacionException(
                    "El RFC no coincide con el nombre y apellidos capturados "
                            + "(se esperaba que empezara con \"" + letrasEsperadas + "\")");
        }

        // --- RFC: fecha de nacimiento ---
        String fechaRfc = rfcUpper.substring(4, 10);
        if (!fechaRfc.equals(fechaEsperada)) {
            throw new ValidacionException(
                    "El RFC no coincide con la fecha de nacimiento capturada "
                            + "(se esperaba la fecha " + fechaEsperada + " en formato AAMMDD)");
        }

        // --- Consistencia cruzada CURP/RFC entre sí ---
        if (!letrasCurp.equals(letrasRfc) || !fechaCurp.equals(fechaRfc)) {
            throw new ValidacionException("La CURP y el RFC capturados no son consistentes entre sí");
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