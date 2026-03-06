package com.fiscalimplify.fiscalimplify.controller;

import com.fiscalimplify.fiscalimplify.documentation.DefaultApiResponses;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller de webhooks da Nuvem Fiscal.
 *
 * @author VendaLume
 * @version 1.0.0
 * @since 2025-02-16
 */
@Tag(name = "Webhook", description = "Recebimento de callbacks da Nuvem Fiscal")
@DefaultApiResponses
@SecurityRequirements
@RestController
@RequestMapping("/webhook")
@Slf4j
public class WebhookController {

    @Operation(summary = "Receber callback da Nuvem Fiscal")
    @PostMapping("/nuvemfiscal")
    public ResponseEntity<Void> receber(@RequestBody Map<String, Object> payload) {
        log.info("Webhook Nuvem Fiscal recebido: tipo={}", payload.get("tipo"));
        log.debug("Payload completo: {}", payload);
        return ResponseEntity.ok().build();
    }
}
