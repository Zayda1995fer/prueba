package com.proyecto.servicios.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class TelefonoMexicanoValidator implements ConstraintValidator<TelefonoMexicano, Long> {

    private String campo;

    @Override
    public void initialize(TelefonoMexicano anotacion) {
        this.campo = anotacion.campo();
    }

    @Override
    public boolean isValid(Long valor, ConstraintValidatorContext contexto) {
        if (valor == null) {
            return true;
        }
        String motivo = motivoInvalido(valor, campo);
        return motivo == null || rechazar(contexto, motivo);
    }

    /** Devuelve el motivo por el que el teléfono no es válido, o null si es válido. */
    static String motivoInvalido(long valor, String campo) {
        String digitos = Long.toString(valor);
        if (valor < 0 || digitos.length() != 10) {
            return campo + " debe contener solo números y tener exactamente 10 dígitos";
        }
        char primero = digitos.charAt(0);
        if (primero == '0' || primero == '1') {
            return campo + " debe tener una lada válida (no puede iniciar con 0 ni con 1)";
        }
        if (digitos.chars().distinct().count() == 1) {
            return campo + " no puede ser una secuencia repetida (por ejemplo 5555555555)";
        }
        return null;
    }

    private boolean rechazar(ConstraintValidatorContext contexto, String mensaje) {
        contexto.disableDefaultConstraintViolation();
        contexto.buildConstraintViolationWithTemplate(mensaje).addConstraintViolation();
        return false;
    }
}