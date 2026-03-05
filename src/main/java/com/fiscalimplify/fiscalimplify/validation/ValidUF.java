package com.fiscalimplify.fiscalimplify.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

/**
 * Valida se o valor é uma sigla de UF válida conforme UnidadeFederativa.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Documented
@Constraint(validatedBy = ValidUFValidator.class)
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidUF {

    String message() default "UF inválida. Use sigla válida (ex: CE, SP)";

    Class<?>[] groups() default {};

    Class<? extends Payload>[] payload() default {};
}
