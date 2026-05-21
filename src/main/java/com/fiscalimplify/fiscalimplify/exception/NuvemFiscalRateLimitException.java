package com.fiscalimplify.fiscalimplify.exception;

/**
 * Nuvem Fiscal retornou HTTP 429 (limite de requisicoes no sandbox/producao).
 */
public class NuvemFiscalRateLimitException extends RuntimeException {

    public NuvemFiscalRateLimitException(String message, Throwable cause) {
        super(message, cause);
    }
}
