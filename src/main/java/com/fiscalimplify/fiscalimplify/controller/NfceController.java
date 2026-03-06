package com.fiscalimplify.fiscalimplify.controller;

import com.fiscalimplify.fiscalimplify.documentation.DefaultApiResponses;
import com.fiscalimplify.fiscalimplify.dto.NfceRequest;
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
 * Controller de emissão de NFC-e e obtenção do PDF via API Nuvem Fiscal.
 *
 * @author VendaLume
 * @version 1.0.0
 * @since 2025-02-16
 */
@Tag(name = "NFC-e", description = "Emissão de NFC-e e obtenção de PDF")
@DefaultApiResponses
@RestController
@RequestMapping("/nfce")
@RequiredArgsConstructor
@Slf4j
public class NfceController {

    private final FiscalService fiscalService;

    @Operation(summary = "Emitir NFC-e")
    @PostMapping
    public ResponseEntity<Map<String, Object>> emitir(@Valid @RequestBody NfceRequest request) {
        return ResponseEntity.ok(fiscalService.emitirNfce(request));
    }


    @Operation(summary = "Obter PDF da NFC-e")
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
