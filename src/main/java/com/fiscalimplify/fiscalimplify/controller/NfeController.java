package com.fiscalimplify.fiscalimplify.controller;

import com.fiscalimplify.fiscalimplify.documentation.DefaultApiResponses;
import com.fiscalimplify.fiscalimplify.dto.NfeRequest;
import com.fiscalimplify.fiscalimplify.service.FiscalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * Controller de emissão de NF-e e obtenção do PDF via API Nuvem Fiscal.
 *
 * @author VendaLume
 * @version 1.0.0
 * @since 2025-02-16
 */
@Tag(name = "NF-e", description = "Emissão de NF-e e obtenção de PDF (DANFE)")
@DefaultApiResponses
@RestController
@RequestMapping("/nfe")
@RequiredArgsConstructor
@Slf4j
public class NfeController {

    private final FiscalService fiscalService;

    @Operation(summary = "Emitir NF-e")
    @PostMapping
    public ResponseEntity<Map<String, Object>> emitir(@Valid @RequestBody NfeRequest request) {
        return ResponseEntity.ok(fiscalService.emitirNfe(request));
    }

    @Operation(summary = "Obter PDF da NF-e (DANFE)")
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
