package com.fiscalimplify.fiscalimplify.controller;

import com.fiscalimplify.fiscalimplify.dto.NfceRequest;
import com.fiscalimplify.fiscalimplify.service.FiscalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller REST responsável pela emissão de NFC-e (Nota Fiscal de Consumidor Eletrônica)
 * e obtenção do PDF (DANFC-e) via API Nuvem Fiscal.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@RestController
@RequestMapping("/nfce")
@RequiredArgsConstructor
@Slf4j
public class NfceController {

    private final FiscalService fiscalService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> emitir(@Valid @RequestBody NfceRequest request) {
        return ResponseEntity.ok(fiscalService.emitirNfce(request));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> obterPdf(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        byte[] pdf = fiscalService.buscarPdfNfce(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String disposition = download ? "attachment" : "inline";
        headers.setContentDispositionFormData(disposition, "nfce-" + id + ".pdf");
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
