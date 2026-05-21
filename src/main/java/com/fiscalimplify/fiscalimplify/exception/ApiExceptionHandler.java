package com.fiscalimplify.fiscalimplify.exception;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Handler global responsável pelo tratamento de exceções da API Fiscalimplify.
 * Converte validações, erros da Nuvem Fiscal e erros genéricos em respostas HTTP padronizadas.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@RestControllerAdvice
@Slf4j
public class ApiExceptionHandler {

    private static String requestLine(WebRequest request) {
        if (request instanceof ServletWebRequest servlet) {
            HttpServletRequest req = servlet.getRequest();
            String q = req.getQueryString();
            return req.getMethod() + " " + req.getRequestURI() + (q != null ? "?" + q : "");
        }
        return "?";
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErroResponse> handleValidation(MethodArgumentNotValidException ex, WebRequest request) {
        log.warn("{} -> 400 validação: {}", requestLine(request), ex.getBindingResult().getFieldErrors());
        List<FieldError> errors = ex.getBindingResult().getFieldErrors();
        Map<String, String> detalhes = errors.stream()
                .collect(Collectors.toMap(FieldError::getField, e -> e.getDefaultMessage() != null ? e.getDefaultMessage() : "inválido"));

        ErroResponse response = new ErroResponse(
                Instant.now(),
                HttpStatus.BAD_REQUEST.value(),
                "Erro de validação",
                detalhes
        );
        return ResponseEntity.badRequest().contentType(MediaType.APPLICATION_JSON).body(response);
    }

    @ExceptionHandler(WebClientResponseException.class)
    public ResponseEntity<ErroResponse> handleNuvemFiscal(WebClientResponseException ex, WebRequest request) {
        log.error("{} -> Nuvem Fiscal {}: {}", requestLine(request), ex.getStatusCode(), ex.getResponseBodyAsString());

        ErroResponse response = new ErroResponse(
                Instant.now(),
                ex.getStatusCode().value(),
                "Erro na comunicação com Nuvem Fiscal",
                Map.of("detalhe", ex.getResponseBodyAsString())
        );
        return ResponseEntity.status(ex.getStatusCode()).contentType(MediaType.APPLICATION_JSON).body(response);
    }

    @ExceptionHandler(RegraNegocioException.class)
    public ResponseEntity<ErroResponse> handleRegraNegocio(RegraNegocioException ex, WebRequest request) {
        log.warn("{} -> 422: {}", requestLine(request), ex.getMessage());
        ErroResponse response = new ErroResponse(
                Instant.now(),
                HttpStatus.UNPROCESSABLE_ENTITY.value(),
                ex.getMessage(),
                Map.of()
        );
        return ResponseEntity.unprocessableEntity().contentType(MediaType.APPLICATION_JSON).body(response);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErroResponse> handleGenerica(Exception ex, WebRequest request) {
        log.error("{} -> 500: {}", requestLine(request), ex.getMessage(), ex);
        ErroResponse response = new ErroResponse(
                Instant.now(),
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                ex.getMessage() != null ? ex.getMessage() : "Erro interno do servidor",
                Map.of()
        );
        return ResponseEntity.status(500)
                .contentType(MediaType.APPLICATION_JSON)
                .body(response);
    }

    public record ErroResponse(Instant timestamp, int status, String mensagem, Map<String, String> detalhes) {
    }
}
