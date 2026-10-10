package com.proyecto.servicios.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.exc.MismatchedInputException;
import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;
import com.proyecto.servicios.model.ErrorCampo;
import com.proyecto.servicios.model.ErrorRespuesta;
import jakarta.validation.ConstraintViolation;
import jakarta.servlet.ServletException;
import jakarta.validation.ConstraintViolationException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSourceResolvable;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.validation.method.ParameterErrors;
import org.springframework.validation.method.ParameterValidationResult;
import org.springframework.web.ErrorResponse;
import org.springframework.web.ErrorResponseException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Un solo lugar que traduce cada excepción a una respuesta HTTP con el
 * mismo formato (ErrorRespuesta). Así ningún controller necesita
 * try/catch y nunca se filtra una traza de error ni un mensaje técnico
 * de la base de datos al cliente.
 *
 * Códigos: 400 datos inválidos · 401 credenciales/sesión · 404 no existe ·
 * 409 duplicado/conflicto · 415 Content-Type incorrecto · 429 demasiados
 * intentos · 500 error interno inesperado.
 *
 * Los errores de validación regresan UN campo a la vez (el primero según
 * el orden en que están declarados en el request), con su nombre, para
 * mostrarlo junto al campo; al corregirlo aparece el siguiente.
 */
@RestControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    private static final String MENSAJE_VALIDACION = "Error de validación";

    // ------------------------------------------------------------------
    // Reglas de negocio (duplicados, no encontrado, credenciales, ...)
    // ------------------------------------------------------------------

    @ExceptionHandler(NegocioException.class)
    public ResponseEntity<ErrorRespuesta> manejarNegocioException(NegocioException ex) {
        log.error("Error de negocio: {} -> HTTP {}", ex.getMessage(), ex.getHttpStatus().value());
        ErrorRespuesta respuesta = base(ex.getHttpStatus(), ex.getMessage());
        respuesta.setField(ex.getCampo());
        return ResponseEntity.status(ex.getHttpStatus()).body(respuesta);
    }

    // ------------------------------------------------------------------
    // Validación del body (@Valid sobre el request)
    // ------------------------------------------------------------------

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacion(MethodArgumentNotValidException ex) {
        Object destino = ex.getBindingResult().getTarget();
        List<Hallazgo> hallazgos = new ArrayList<>();
        for (FieldError error : ex.getBindingResult().getFieldErrors()) {
            hallazgos.add(new Hallazgo(error.getField(), error.getDefaultMessage(),
                    posicion(destino == null ? null : destino.getClass(), error.getField())));
        }
        return respuestaValidacion(hallazgos);
    }

    // Validación de @PathVariable / @RequestParam (p. ej. @Positive, @Pattern).
    // Puede traer también errores del body cuando el método tiene ambos.
    @ExceptionHandler(HandlerMethodValidationException.class)
    public ResponseEntity<ErrorRespuesta> manejarValidacionDeMetodo(HandlerMethodValidationException ex) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        int indiceParametro = 0;
        for (ParameterValidationResult resultado : ex.getAllValidationResults()) {
            if (resultado instanceof ParameterErrors errores) {
                Class<?> tipo = errores.getArgument() == null ? null : errores.getArgument().getClass();
                for (FieldError error : errores.getFieldErrors()) {
                    hallazgos.add(new Hallazgo(error.getField(), error.getDefaultMessage(),
                            posicion(tipo, error.getField())));
                }
            } else {
                String nombre = resultado.getMethodParameter().getParameterName();
                for (MessageSourceResolvable error : resultado.getResolvableErrors()) {
                    hallazgos.add(new Hallazgo(nombre == null ? "parametro" : nombre, error.getDefaultMessage(),
                            List.of(-1, indiceParametro)));
                }
            }
            indiceParametro++;
        }
        return respuestaValidacion(hallazgos);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarViolaciones(ConstraintViolationException ex) {
        List<Hallazgo> hallazgos = new ArrayList<>();
        int i = 0;
        for (ConstraintViolation<?> violacion : ex.getConstraintViolations()) {
            String ruta = violacion.getPropertyPath().toString();
            hallazgos.add(new Hallazgo(ruta.substring(ruta.lastIndexOf('.') + 1), violacion.getMessage(),
                    List.of(-1, i++)));
        }
        return respuestaValidacion(hallazgos);
    }

    // ------------------------------------------------------------------
    // JSON mal formado, campos desconocidos o de tipo incorrecto
    // ------------------------------------------------------------------

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorRespuesta> manejarJsonInvalido(HttpMessageNotReadableException ex) {
        String campo = null;
        String mensaje;
        Throwable causa = ex.getCause();

        if (causa instanceof UnrecognizedPropertyException desconocida) {
            campo = rutaCompleta(desconocida) ;
            // Si el campo desconocido viene dentro de un objeto (p. ej. domicilio), se indica de cuál.
            String objeto = rutaObjetoPadre(campo);
            mensaje = objeto == null
                    ? "El campo '" + desconocida.getPropertyName() + "' no está permitido en el objeto de la petición"
                    : "El campo '" + desconocida.getPropertyName() + "' no está permitido en el objeto '" + objeto + "'";
        } else if (causa instanceof JsonMappingException mapeo) {
            campo = rutaCompleta(mapeo);
            mensaje = mensajePorTipo(campo, mapeo);
        } else if (causa instanceof JsonProcessingException) {
            mensaje = "El cuerpo de la petición no es un JSON válido";
        } else {
            mensaje = "El cuerpo de la petición es obligatorio y debe ser un JSON válido";
        }

        // Se registra solo el campo y el tipo de error, nunca el contenido
        // recibido (podría traer contraseñas u otros datos sensibles).
        log.error("JSON de entrada inválido (campo: {}, causa: {})", campo,
                causa == null ? "sin cuerpo" : causa.getClass().getSimpleName());

        return respuestaValidacion(List.of(new Hallazgo(campo, mensaje, List.of(0))));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorRespuesta> manejarTipoIncorrecto(MethodArgumentTypeMismatchException ex) {
        Class<?> tipo = ex.getRequiredType();
        String esperado;
        if (tipo == LocalDateTime.class) {
            esperado = "una fecha y hora con formato AAAA-MM-DDThh:mm:ss";
        } else if (tipo == LocalDate.class) {
            esperado = "una fecha con formato AAAA-MM-DD";
        } else if (tipo == Integer.class || tipo == Long.class || tipo == int.class || tipo == long.class) {
            esperado = "un número entero válido";
        } else if (tipo == Boolean.class || tipo == boolean.class) {
            esperado = "true o false";
        } else {
            esperado = "un valor válido";
        }
        return respuestaValidacion(List.of(new Hallazgo(ex.getName(),
                "El parámetro '" + ex.getName() + "' debe ser " + esperado, List.of(0))));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorRespuesta> manejarParametroFaltante(MissingServletRequestParameterException ex) {
        return respuestaValidacion(List.of(new Hallazgo(ex.getParameterName(),
                "El parámetro '" + ex.getParameterName() + "' es obligatorio", List.of(0))));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<ErrorRespuesta> manejarContentType(HttpMediaTypeNotSupportedException ex) {
        log.error("Content-Type no soportado: {}", ex.getContentType());
        ErrorRespuesta respuesta = base(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
                "El Content-Type de la petición debe ser application/json");
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE).body(respuesta);
    }

    // Última defensa: si algo llegara a la base de datos y una restricción
    // (UNIQUE, CHECK, FOREIGN KEY) lo rechaza, el cliente recibe un mensaje
    // comprensible y NO el texto técnico de PostgreSQL.
    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorRespuesta> manejarIntegridad(DataIntegrityViolationException ex) {
        log.error("Violación de integridad en base de datos: {}", ex.getMostSpecificCause().getClass().getSimpleName());
        ErrorRespuesta respuesta = base(HttpStatus.CONFLICT,
                "Los datos enviados ya existen o no cumplen una regla de integridad");
        return ResponseEntity.status(HttpStatus.CONFLICT).body(respuesta);
    }

    // ------------------------------------------------------------------
    // Resto de errores
    // ------------------------------------------------------------------

    // Excepciones estándar de Spring MVC (ruta inexistente, método HTTP no
    // permitido, ...): conservan su código HTTP en lugar de volverse 500.
    @ExceptionHandler({ServletException.class, ErrorResponseException.class})
    public ResponseEntity<ErrorRespuesta> manejarErrorSpring(Exception ex) {
        HttpStatus status = ex instanceof ErrorResponse er
                ? HttpStatus.resolve(er.getStatusCode().value())
                : HttpStatus.INTERNAL_SERVER_ERROR;
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        if (status.is5xxServerError()) {
            log.error("Error interno de servlet", ex);
        }
        String mensaje;
        if (status == HttpStatus.NOT_FOUND) {
            mensaje = "El recurso solicitado no existe";
        } else if (status == HttpStatus.METHOD_NOT_ALLOWED) {
            mensaje = "El método HTTP no está permitido para esta ruta";
        } else if (status == HttpStatus.NOT_ACCEPTABLE) {
            mensaje = "El formato de respuesta solicitado no está disponible";
        } else if (status.is4xxClientError()) {
            mensaje = "La petición no es válida";
        } else {
            mensaje = "Ocurrió un error interno. Intenta de nuevo más tarde";
        }
        return ResponseEntity.status(status).body(base(status, mensaje));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorRespuesta> manejarErrorInesperado(Exception ex) {
        // La traza completa queda SOLO en el log del servidor
        log.error("Error interno no controlado", ex);
        ErrorRespuesta respuesta = base(HttpStatus.INTERNAL_SERVER_ERROR,
                "Ocurrió un error interno. Intenta de nuevo más tarde");
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(respuesta);
    }

    // ------------------------------------------------------------------
    // Apoyo
    // ------------------------------------------------------------------

    private ErrorRespuesta base(HttpStatus status, String mensaje) {
        ErrorRespuesta respuesta = new ErrorRespuesta();
        respuesta.setSuccess(false);
        respuesta.setCodigo(status.value());
        respuesta.setMessage(mensaje);
        return respuesta;
    }

    /** Elige el primer hallazgo (por orden de declaración) y arma la respuesta 400. */
    private ResponseEntity<ErrorRespuesta> respuestaValidacion(List<Hallazgo> hallazgos) {
        Hallazgo primero = hallazgos.stream()
                .min(Comparator.comparing(Hallazgo::posicion, GlobalExceptionHandler::compararPosiciones))
                .orElse(null);

        log.error("Error de validación de datos de entrada: {} error(es), se reporta el primero: {}",
                hallazgos.size(), primero == null ? "n/a" : primero.campo() + " -> " + primero.mensaje());

        ErrorRespuesta respuesta = base(HttpStatus.BAD_REQUEST, MENSAJE_VALIDACION);
        if (primero != null) {
            respuesta.setErrors(List.of(new ErrorCampo(primero.campo(), primero.mensaje())));
        }
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(respuesta);
    }

    private String rutaCompleta(JsonMappingException ex) {
        String ruta = ex.getPath().stream()
                .map(ref -> ref.getFieldName() != null ? ref.getFieldName() : "[" + ref.getIndex() + "]")
                .collect(Collectors.joining("."))
                .replace(".[", "[");
        if (ex instanceof UnrecognizedPropertyException desconocida) {
            // La ruta de esta excepción termina en el campo desconocido ya incluido
            return ruta.isEmpty() ? desconocida.getPropertyName() : ruta;
        }
        return ruta.isEmpty() ? null : ruta;
    }

    // "domicilio.xyz" -> "domicilio"; "nacionalidad[0].xyz" -> "nacionalidad[0]"; "xyz" -> null
    private String rutaObjetoPadre(String ruta) {
        if (ruta == null) {
            return null;
        }
        int punto = ruta.lastIndexOf('.');
        return punto < 0 ? null : ruta.substring(0, punto);
    }

    private String mensajePorTipo(String campo, JsonMappingException ex) {
        String nombre = campo == null ? "el campo" : "'" + campo + "'";
        String minusculas = campo == null ? "" : campo.toLowerCase();

        if (minusculas.contains("telefono")) {
            String cual = minusculas.contains("alternativo") ? "teléfono alternativo" : "teléfono móvil";
            return "El " + cual + " solo debe contener números, con formato de 10 dígitos y lada válida";
        }
        Class<?> destino = ex instanceof MismatchedInputException mismatched ? mismatched.getTargetType() : null;
        if (destino == null) {
            return "El valor de " + nombre + " no tiene un formato válido";
        }
        // Propiedades que son objetos o arreglos (domicilio, nacionalidad)
        if (java.util.Collection.class.isAssignableFrom(destino) || destino.isArray()) {
            return "El objeto " + nombre + " debe enviarse como un arreglo de objetos, por ejemplo [{\"id\": 1, \"nombre\": \"Mexicana\"}]";
        }
        if (!destino.isPrimitive() && !destino.getName().startsWith("java.") && !destino.isEnum()) {
            return "El objeto " + nombre + " debe enviarse como un objeto JSON con sus propiedades entre llaves { }";
        }
        if (destino == LocalDate.class) {
            return "El valor de " + nombre + " debe ser una fecha real con formato AAAA-MM-DD";
        }
        if (destino == BigDecimal.class || destino == Double.class || destino == double.class) {
            return "El valor de " + nombre + " debe ser un número válido, sin letras ni símbolos (usa punto para decimales)";
        }
        if (Number.class.isAssignableFrom(destino) || destino == int.class || destino == long.class) {
            return "El valor de " + nombre + " debe ser un número entero válido, sin letras ni símbolos";
        }
        if (destino == Boolean.class || destino == boolean.class) {
            return "El valor de " + nombre + " debe ser true o false (sin comillas)";
        }
        if (destino == String.class) {
            return "El valor de " + nombre + " debe ser un texto";
        }
        return "El valor de " + nombre + " no tiene un formato válido";
    }

    // Posición del campo dentro de la clase (y de sus clases anidadas,
    // p. ej. "domicilio.calle"), para ordenar los errores igual que el request.
    private List<Integer> posicion(Class<?> tipo, String ruta) {
        List<Integer> posiciones = new ArrayList<>();
        Class<?> actual = tipo;
        for (String segmento : ruta.split("\\.")) {
            String nombre = segmento.replaceAll("\\[.*]", "");
            int indice = Integer.MAX_VALUE;
            Class<?> siguiente = null;
            if (actual != null) {
                Field[] campos = actual.getDeclaredFields();
                for (int i = 0; i < campos.length; i++) {
                    if (campos[i].getName().equals(nombre)) {
                        indice = i;
                        siguiente = campos[i].getType();
                        if (List.class.isAssignableFrom(siguiente)
                                && campos[i].getGenericType() instanceof java.lang.reflect.ParameterizedType pt
                                && pt.getActualTypeArguments().length == 1
                                && pt.getActualTypeArguments()[0] instanceof Class<?> elemento) {
                            siguiente = elemento;   // List<NacionalidadRequest> -> NacionalidadRequest
                        }
                        break;
                    }
                }
            }
            posiciones.add(indice);
            actual = siguiente;
        }
        return posiciones;
    }

    private static int compararPosiciones(List<Integer> a, List<Integer> b) {
        for (int i = 0; i < Math.min(a.size(), b.size()); i++) {
            int c = Integer.compare(a.get(i), b.get(i));
            if (c != 0) {
                return c;
            }
        }
        return Integer.compare(a.size(), b.size());
    }

    private record Hallazgo(String campo, String mensaje, List<Integer> posicion) {
    }
}