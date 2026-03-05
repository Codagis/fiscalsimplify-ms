package com.fiscalimplify.fiscalimplify.controller;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * Controller REST que recebe webhooks da Nuvem Fiscal para notificações de status
 * de documentos fiscais (NF-e, NFC-e) e demais eventos.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@RestController
@RequestMapping("/webhook")
@Slf4j
public class WebhookController {

    @PostMapping("/nuvemfiscal")
    public ResponseEntity<Void> receber(@RequestBody Map<String, Object> payload) {
        log.info("Webhook Nuvem Fiscal recebido: tipo={}", payload.get("tipo"));
        log.debug("Payload completo: {}", payload);
        return ResponseEntity.ok().build();
    }
}
