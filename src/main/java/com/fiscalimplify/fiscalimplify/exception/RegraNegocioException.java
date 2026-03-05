package com.fiscalimplify.fiscalimplify.exception;

/**
 * Exceção responsável por representar violações de regras de negócio da API Fiscalimplify.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
public class RegraNegocioException extends RuntimeException {

    public RegraNegocioException(String mensagem) {
        super(mensagem);
    }

    public RegraNegocioException(String mensagem, Throwable causa) {
        super(mensagem, causa);
    }
}
