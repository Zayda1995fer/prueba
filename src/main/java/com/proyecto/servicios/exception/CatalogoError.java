package com.proyecto.servicios.exception;

import org.springframework.http.HttpStatus;

/**
 * Catálogo único de errores de la aplicación. Cada entrada define, en un
 * solo lugar: el código HTTP, el campo del request al que se refiere (si
 * aplica) y la plantilla del mensaje. Los marcadores %s se rellenan con
 * los argumentos que recibe NegocioException.
 *
 * Para agregar un error nuevo solo se agrega una línea aquí; no se crea
 * una clase de excepción por cada caso.
 */
public enum CatalogoError {

    // ---------------- No encontrado (404) ----------------
    CLIENTE_NO_ENCONTRADO(HttpStatus.NOT_FOUND, null, "No se encontró el cliente: %s"),
    CUENTA_NO_ENCONTRADA(HttpStatus.NOT_FOUND, null, "No se encontró la cuenta con número: %s"),

    // ---------------- Duplicados / conflicto (409) ----------------
    CURP_DUPLICADA(HttpStatus.CONFLICT, "curp", "Ya existe un cliente registrado con la CURP %s"),
    RFC_DUPLICADO(HttpStatus.CONFLICT, "rfc", "Ya existe un cliente registrado con el RFC %s"),
    CORREO_DUPLICADO(HttpStatus.CONFLICT, "correoElectronico", "Ya existe un cliente registrado con el correo %s"),
    USUARIO_EN_USO(HttpStatus.CONFLICT, "usuario", "El usuario '%s' ya está en uso"),
    CLIENTE_CON_ACCESO(HttpStatus.CONFLICT, null, "El cliente ya cuenta con credenciales de acceso"),
    INTEGRIDAD_DATOS(HttpStatus.CONFLICT, null, "Los datos enviados ya existen o no cumplen una regla de integridad"),

    // ---------------- Autenticación / sesión (401, 429) ----------------
    CREDENCIALES_INVALIDAS(HttpStatus.UNAUTHORIZED, null, "Usuario o contraseña incorrectos"),
    ACCESO_INACTIVO(HttpStatus.UNAUTHORIZED, null, "El acceso se encuentra inactivo"),
    TOKEN_INVALIDO(HttpStatus.UNAUTHORIZED, null, "Token de sesión inválido"),
    SESION_EXPIRADA(HttpStatus.UNAUTHORIZED, null, "La sesión expiró por inactividad (límite de %s minutos)"),
    DEMASIADOS_INTENTOS(HttpStatus.TOO_MANY_REQUESTS, null, "Demasiados intentos fallidos. Intenta de nuevo en %s minuto(s)"),

    // ---------------- Reglas de negocio de Clientes / Login (400) ----------------
    RANGO_FECHAS_INVALIDO(HttpStatus.BAD_REQUEST, "fechaCreacionDesde",
            "La fecha 'fechaCreacionDesde' no puede ser posterior a 'fechaCreacionHasta'"),
    CLIENTE_MENOR_DE_EDAD(HttpStatus.BAD_REQUEST, "fechaNacimiento",
            "El cliente debe ser mayor de edad (18 años o más)"),
    EDAD_MAXIMA_EXCEDIDA(HttpStatus.BAD_REQUEST, "fechaNacimiento",
            "La fecha de nacimiento no es válida: la edad no puede ser mayor a %s años"),
    TELEFONOS_IGUALES(HttpStatus.BAD_REQUEST, "telefonoAlternativo",
            "El teléfono alternativo no puede ser igual al teléfono móvil"),
    NACIONALIDAD_NO_EXISTE(HttpStatus.BAD_REQUEST, "nacionalidad[0].id",
            "La nacionalidad indicada (id %s) no existe en el catálogo. "
                    + "Consulta GET /nacionalidades para ver las nacionalidades disponibles."),
    NACIONALIDAD_NOMBRE_NO_COINCIDE(HttpStatus.BAD_REQUEST, "nacionalidad[0].nombre",
            "El nombre de la nacionalidad \"%s\" no corresponde al id %s del catálogo (\"%s\"). "
                    + "Puede escribirse en masculino o femenino, sin importar mayúsculas ni acentos."),
    NACIONALIDAD_CLIENTE_INEXISTENTE(HttpStatus.BAD_REQUEST, null,
            "El cliente tiene un nacionalidad_id (%s) que ya no existe en el catálogo"),
    SALDO_INICIAL_NEGATIVO(HttpStatus.BAD_REQUEST, null, "El saldo inicial configurado no puede ser negativo"),
    CLIENTE_INACTIVO(HttpStatus.BAD_REQUEST, null, "No se puede crear acceso para un cliente inactivo"),
    CONTRASENA_CONTIENE_USUARIO(HttpStatus.BAD_REQUEST, "contrasena",
            "La contraseña no puede contener el nombre de usuario"),

    // ---------------- Coherencia CURP / RFC (400) ----------------
    CURP_LONGITUD(HttpStatus.BAD_REQUEST, "curp", "La CURP no tiene la longitud suficiente para validarse"),
    CURP_NOMBRE(HttpStatus.BAD_REQUEST, "curp",
            "La CURP no coincide con el nombre y apellidos capturados (se esperaba que empezara con \"%s\", "
                    + "de acuerdo al apellido paterno, apellido materno y nombre)"),
    CURP_FECHA(HttpStatus.BAD_REQUEST, "curp",
            "La CURP no coincide con la fecha de nacimiento capturada (se esperaba la fecha %s en formato AAMMDD)"),
    SEXO_INVALIDO(HttpStatus.BAD_REQUEST, "sexo",
            "El sexo debe capturarse como \"H\" o \"M\" para poder validar la CURP"),
    CURP_SEXO(HttpStatus.BAD_REQUEST, "sexo",
            "La CURP no coincide con el sexo capturado (la CURP indica \"%s\" y se capturó \"%s\")"),
    RFC_LONGITUD(HttpStatus.BAD_REQUEST, "rfc", "El RFC no tiene la longitud suficiente para validarse"),
    RFC_NOMBRE(HttpStatus.BAD_REQUEST, "rfc",
            "El RFC no coincide con el nombre y apellidos capturados (se esperaba que empezara con \"%s\")"),
    RFC_FECHA(HttpStatus.BAD_REQUEST, "rfc",
            "El RFC no coincide con la fecha de nacimiento capturada (se esperaba la fecha %s en formato AAMMDD)"),
    CURP_RFC_INCONSISTENTES(HttpStatus.BAD_REQUEST, "rfc",
            "La CURP y el RFC capturados no son consistentes entre sí"),

    // ---------------- Cifrado de datos sensibles (400) ----------------
    DATO_SENSIBLE_PROTEGER(HttpStatus.BAD_REQUEST, null, "No fue posible proteger el dato sensible"),
    DATO_SENSIBLE_LEER(HttpStatus.BAD_REQUEST, null, "No fue posible leer el dato sensible"),
    DATO_SENSIBLE_PROCESAR(HttpStatus.BAD_REQUEST, null, "No fue posible procesar el dato sensible"),

    // ---------------- Errores genéricos de la API (los usa GlobalExceptionHandler) ----------------
    ERROR_VALIDACION(HttpStatus.BAD_REQUEST, null, "Error de validación"),
    JSON_INVALIDO(HttpStatus.BAD_REQUEST, null, "El cuerpo de la petición no es un JSON válido"),
    CUERPO_OBLIGATORIO(HttpStatus.BAD_REQUEST, null,
            "El cuerpo de la petición es obligatorio y debe ser un JSON válido"),
    CAMPO_NO_PERMITIDO(HttpStatus.BAD_REQUEST, null,
            "El campo '%s' no está permitido en el objeto de la petición"),
    CAMPO_NO_PERMITIDO_EN_OBJETO(HttpStatus.BAD_REQUEST, null,
            "El campo '%s' no está permitido en el objeto '%s'"),
    CONTENT_TYPE_INVALIDO(HttpStatus.UNSUPPORTED_MEDIA_TYPE, null,
            "El Content-Type de la petición debe ser application/json"),
    RECURSO_NO_EXISTE(HttpStatus.NOT_FOUND, null, "El recurso solicitado no existe"),
    METODO_NO_PERMITIDO(HttpStatus.METHOD_NOT_ALLOWED, null, "El método HTTP no está permitido para esta ruta"),
    FORMATO_NO_DISPONIBLE(HttpStatus.NOT_ACCEPTABLE, null, "El formato de respuesta solicitado no está disponible"),
    PETICION_INVALIDA(HttpStatus.BAD_REQUEST, null, "La petición no es válida"),
    ERROR_INTERNO(HttpStatus.INTERNAL_SERVER_ERROR, null, "Ocurrió un error interno. Intenta de nuevo más tarde");

    private final HttpStatus httpStatus;
    private final String campo;
    private final String plantilla;

    CatalogoError(HttpStatus httpStatus, String campo, String plantilla) {
        this.httpStatus = httpStatus;
        this.campo = campo;
        this.plantilla = plantilla;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    /** Campo del request al que se refiere el error por defecto; null si no aplica. */
    public String getCampo() {
        return campo;
    }

    /** Mensaje con los marcadores %s sustituidos por los argumentos. */
    public String mensaje(Object... argumentos) {
        return argumentos == null || argumentos.length == 0 ? plantilla : String.format(plantilla, argumentos);
    }
}
