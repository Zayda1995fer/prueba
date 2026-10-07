package com.proyecto.servicios.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Teléfono a 10 dígitos con lada válida.
 */
@Documented
@Constraint(validatedBy = TelefonoMexicanoValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface TelefonoMexicano {

    String message() default "El teléfono no es válido";

    /** Texto con el que empiezan los mensajes, p. ej. "El teléfono móvil". */
    String campo() default "El teléfono";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}