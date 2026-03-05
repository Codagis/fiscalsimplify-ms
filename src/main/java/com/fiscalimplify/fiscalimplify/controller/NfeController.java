package com.fiscalimplify.fiscalimplify.controller;

import com.fiscalimplify.fiscalimplify.dto.NfeRequest;
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
 * Controller REST responsável pela emissão de NF-e (Nota Fiscal Eletrônica)
 * e obtenção do PDF (DANFE) via API Nuvem Fiscal.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@RestController
@RequestMapping("/nfe")
@RequiredArgsConstructor
@Slf4j
public class NfeController {

    private final FiscalService fiscalService;

    @PostMapping
    public ResponseEntity<Map<String, Object>> emitir(@Valid @RequestBody NfeRequest request) {
        return ResponseEntity.ok(fiscalService.emitirNfe(request));
    }

    @GetMapping("/{id}/pdf")
    public ResponseEntity<byte[]> obterPdf(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        byte[] pdf = fiscalService.buscarPdfNfe(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String disposition = download ? "attachment" : "inline";
        headers.setContentDispositionFormData(disposition, "nfe-" + id + ".pdf");
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok()
                .headers(headers)
                .body(pdf);
    }
}
