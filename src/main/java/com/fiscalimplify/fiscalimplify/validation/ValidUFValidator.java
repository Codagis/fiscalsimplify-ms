package com.fiscalimplify.fiscalimplify.validation;

import com.fiscalimplify.fiscalimplify.enums.UnidadeFederativa;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

/**
 * Validador que verifica se a sigla da UF pertence ao enum UnidadeFederativa.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
public class ValidUFValidator implements ConstraintValidator<ValidUF, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null || value.isBlank()) {
            return true;
        }
        return UnidadeFederativa.isValid(value);
    }
}
