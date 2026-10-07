package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.Set;

public class ContrasenaSeguraValidator implements ConstraintValidator<ContrasenaSegura, String> {

    // Palabras base de las contraseñas más usadas. Se compara contra la
    // contraseña sin números, símbolos ni acentos, para atrapar variantes
    // como "Password1!" o "Qwerty123#".
    private static final Set<String> COMUNES = Set.of(
            "password", "contrasena", "qwerty", "qwertyuiop", "admin", "administrador",
            "welcome", "bienvenido", "letmein", "iloveyou", "teamo", "monkey", "dragon",
            "login", "usuario", "master", "abc", "abcdef", "abcdefgh", "asdfgh", "asdfghjkl",
            "zxcvbn", "football", "futbol", "superman", "baseball", "sunshine", "princess");

    @Override
    public boolean isValid(String valor, ConstraintValidatorContext contexto) {
        if (valor == null) {
            return true;
        }
        String motivo = motivoInvalido(valor);
        if (motivo == null) {
            return true;
        }
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(motivo).addConstraintViolation();
        return false;
    }

    /** Devuelve la primera regla que incumple la contraseña, o null si es válida. */
    static String motivoInvalido(String valor) {
        if (valor.length() < ReglasValidacion.CONTRASENA_MIN) {
            return "La contraseña debe tener al menos " + ReglasValidacion.CONTRASENA_MIN + " caracteres";
        }
        if (valor.getBytes(StandardCharsets.UTF_8).length > ReglasValidacion.CONTRASENA_MAX) {
            return "La contraseña no puede exceder " + ReglasValidacion.CONTRASENA_MAX + " caracteres";
        }
        if (valor.chars().anyMatch(Character::isWhitespace)) {
            return "La contraseña no puede contener espacios";
        }
        if (valor.chars().noneMatch(Character::isUpperCase)) {
            return "La contraseña debe incluir al menos una letra mayúscula";
        }
        if (valor.chars().noneMatch(Character::isLowerCase)) {
            return "La contraseña debe incluir al menos una letra minúscula";
        }
        if (valor.chars().noneMatch(Character::isDigit)) {
            return "La contraseña debe incluir al menos un número";
        }
        if (valor.chars().allMatch(Character::isLetterOrDigit)) {
            return "La contraseña debe incluir al menos un carácter especial (por ejemplo ! # % & * . _ -)";
        }
        if (esComun(valor)) {
            return "La contraseña es demasiado común, elige una menos predecible";
        }
        return null;
    }

    private static boolean esComun(String valor) {
        String soloLetras = Normalizer.normalize(valor.toLowerCase(), Normalizer.Form.NFD)
                .replaceAll("[^a-z]", "");
        return COMUNES.contains(soloLetras);
    }
}