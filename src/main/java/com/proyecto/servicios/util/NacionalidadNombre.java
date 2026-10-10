package com.proyecto.servicios.util;

import java.text.Normalizer;
import java.util.Locale;


public final class NacionalidadNombre {

    private NacionalidadNombre() {
    }

    public static boolean equivalentes(String capturado, String catalogo) {
        if (capturado == null || catalogo == null) {
            return false;
        }
        String a = normalizar(capturado);
        String b = normalizar(catalogo);
        if (a.isEmpty() || b.isEmpty()) {
            return false;
        }
        if (a.equals(b)) {
            return true;
        }
        // Masculino terminado en "o": mexicano -> mexicana, chino -> china
        if (a.endsWith("o") && (a.substring(0, a.length() - 1) + "a").equals(b)) {
            return true;
        }
        // Masculino terminado en consonante: español -> española, alemán -> alemana,
        // francés -> francesa, portugués -> portuguesa
        return b.equals(a + "a");
    }

    private static String normalizar(String texto) {
        String sinAcentos = Normalizer.normalize(texto.trim().toLowerCase(Locale.ROOT), Normalizer.Form.NFD)
                .replaceAll("\\p{InCombiningDiacriticalMarks}+", "");
        return sinAcentos.replaceAll("\\s+", " ");
    }
}