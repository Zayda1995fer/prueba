package com.proyecto.servicios.validation;

public final class ReglasValidacion {

    private ReglasValidacion() {
    }

    // Letras del español (con acentos, diéresis y ñ)
    public static final String LETRAS = "A-Za-zÁÉÍÓÚÜÑáéíóúüñ";

    // Palabras separadas por UN solo espacio (el trim ya quitó los de las puntas)
    public static final String NOMBRE_PERSONA = "^[" + LETRAS + "]+( [" + LETRAS + "]+)*$";

    public static final String OCUPACION = "^[" + LETRAS + "]+( [" + LETRAS + "]+)*$";

    public static final String EMPRESA = "^[" + LETRAS + "0-9 .,&'-]+$";

    public static final String CURP = "^[A-Z]{4}[0-9]{6}[HM][A-Z]{5}[A-Z0-9]{2}$";

    // RFC de persona FÍSICA: 4 letras + fecha AAMMDD + homoclave de 3 = 13 caracteres
    public static final String RFC_PERSONA_FISICA = "^[A-ZÑ&]{4}[0-9]{6}[A-Z0-9]{3}$";

    public static final String SEXO = "^[HM]$";

    public static final String ESTADO_CIVIL =
            "^(Soltero\\(a\\)|Casado\\(a\\)|Divorciado\\(a\\)|Viudo\\(a\\)|Unión libre)$";

    // Un solo @, dominio con al menos un punto y extensión de 2+ letras,
    // sin espacios ni puntos seguidos. Se evalúa sobre el correo YA en minúsculas.
    public static final String CORREO =
            "^(?!.*\\.\\.)[a-z0-9]([a-z0-9._%+-]*[a-z0-9])?@[a-z0-9]([a-z0-9-]*[a-z0-9])?"
                    + "(\\.[a-z0-9]([a-z0-9-]*[a-z0-9])?)*\\.[a-z]{2,}$";

    public static final String USUARIO = "^[A-Za-z0-9._-]+$";

    // --- Domicilio ---
    public static final String CALLE = "^[" + LETRAS + "0-9 #.,'-]+$";
    public static final String NUMERO_DOMICILIO = "^[" + LETRAS + "0-9#/-]+$";   // 123, 45-B, S/N
    public static final String COLONIA = "^[" + LETRAS + "0-9 .,'-]+$";
    public static final String SOLO_LETRAS_Y_SIGNOS = "^[" + LETRAS + " .'-]+$";  // municipio, estado, país
    public static final String CODIGO_POSTAL = "^[0-9]{5}$";

    // --- Cuenta / sesión (valores generados por el sistema) ---
    public static final String NUMERO_CUENTA = "^[0-9]{10}$";
    public static final String TOKEN_SESION =
            "^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$";

    public static final String BOOLEANO_TEXTO = "^(true|false)$";

    // --- Límites ---
    public static final int EDAD_MAXIMA = 120;
    public static final int CONTRASENA_MIN = 8;
    public static final int CONTRASENA_MAX = 72; // BCrypt solo procesa los primeros 72 bytes
}