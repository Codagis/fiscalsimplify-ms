package com.fiscalimplify.fiscalimplify.controller;

import com.fiscalimplify.fiscalimplify.documentation.DefaultApiResponses;
import com.fiscalimplify.fiscalimplify.dto.NfeImportXmlResponse;
import com.fiscalimplify.fiscalimplify.dto.NfeRequest;
import com.fiscalimplify.fiscalimplify.service.FiscalService;
import com.fiscalimplify.fiscalimplify.service.NfeXmlImportService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

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
    private final com.fiscalimplify.fiscalimplify.integration.nuvemfiscal.NuvemFiscalService nuvemFiscalService;
    private final NfeXmlImportService nfeXmlImportService;

    @Operation(summary = "Emitir NF-e")
    @PostMapping
    public ResponseEntity<Map<String, Object>> emitir(@Valid @RequestBody NfeRequest request) {
        return ResponseEntity.ok(fiscalService.emitirNfe(request));
    }

    @Operation(summary = "Emitir NF-e a partir de XML (upload)")
    @PostMapping(value = "/import-xml", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<NfeImportXmlResponse> importarXml(@RequestPart MultipartFile xml) {
        return ResponseEntity.ok(nfeXmlImportService.emitirNfeFromXml(xml));
    }

    @Operation(summary = "Listar NF-e emitidas (beneficiária/emitente)")
    @GetMapping
    public ResponseEntity<Map<String, Object>> listarEmitidas(
            @RequestParam String cnpj,
            @RequestParam String ambiente,
            @RequestParam(required = false, name = "$top") Integer top,
            @RequestParam(required = false, name = "$skip") Integer skip,
            @RequestParam(required = false, name = "$inlinecount") Boolean inlinecount,
            @RequestParam(required = false) String referencia,
            @RequestParam(required = false) String chave,
            @RequestParam(required = false) String serie
    ) {
        String cnpjLimpo = cnpj != null ? cnpj.replaceAll("\\D", "") : "";
        return ResponseEntity.ok(nuvemFiscalService.listarNfeEmitidas(cnpjLimpo, ambiente, top, skip, inlinecount, referencia, chave, serie));
    }

    @Operation(summary = "Detalhar NF-e por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Map<String, Object>> detalhar(@PathVariable String id) {
        return ResponseEntity.ok(nuvemFiscalService.buscarNfePorId(id));
    }

    @Operation(summary = "Solicitar distribuição NF-e na SEFAZ (buscar notas em que a empresa é destinatária)")
    @PostMapping("/received/sync")
    public ResponseEntity<Map<String, Object>> sincronizarRecebidas(
            @RequestParam String cnpj,
            @RequestParam String ambiente,
            @RequestParam String ufAutor,
            @RequestParam(required = false, name = "dist_nsu") Integer distNsu
    ) {
        String cnpjLimpo = cnpj != null ? cnpj.replaceAll("\\D", "") : "";
        return ResponseEntity.ok(nuvemFiscalService.solicitarDistribuicaoNfe(cnpjLimpo, ambiente, ufAutor, distNsu));
    }

    @Operation(summary = "Listar NF-e recebidas via distribuição (a pagar/destinatária)")
    @GetMapping("/received")
    public ResponseEntity<Map<String, Object>> listarRecebidas(
            @RequestParam String cnpj,
            @RequestParam String ambiente,
            @RequestParam(required = false, name = "$top") Integer top,
            @RequestParam(required = false, name = "$skip") Integer skip,
            @RequestParam(required = false, name = "$inlinecount") Boolean inlinecount,
            @RequestParam(required = false, name = "dist_nsu") Integer distNsu,
            @RequestParam(required = false, name = "forma_distribuicao") String formaDistribuicao,
            @RequestParam(required = false, name = "chave_acesso") String chaveAcesso
    ) {
        String cnpjLimpo = cnpj != null ? cnpj.replaceAll("\\D", "") : "";
        log.info("NFE(recebidas): request cnpj={} ambiente={} top={} skip={} inlinecount={} dist_nsu={} forma_distribuicao={} chave_acesso={}",
                cnpjLimpo, ambiente, top, skip, inlinecount, distNsu, formaDistribuicao, (chaveAcesso != null && !chaveAcesso.isBlank()));
        return ResponseEntity.ok(nuvemFiscalService.listarNfeRecebidasDistribuicao(
                cnpjLimpo, ambiente, top, skip, inlinecount, distNsu, formaDistribuicao, chaveAcesso));
    }

    @Operation(summary = "Detalhar documento de distribuição NF-e por ID")
    @GetMapping("/received/{id}")
    public ResponseEntity<Map<String, Object>> detalharRecebida(@PathVariable String id) {
        return ResponseEntity.ok(nuvemFiscalService.buscarDocumentoDistribuicaoNfe(id));
    }

    @Operation(summary = "Baixar PDF do documento de distribuição NF-e por ID")
    @GetMapping(value = "/received/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    public ResponseEntity<byte[]> baixarPdfRecebida(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        byte[] pdf = nuvemFiscalService.baixarPdfDocumentoDistribuicaoNfe(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_PDF);
        String disposition = download ? "attachment" : "inline";
        headers.setContentDispositionFormData(disposition, "nfe-recebida-" + id + ".pdf");
        headers.setContentLength(pdf.length);
        return ResponseEntity.ok().headers(headers).body(pdf);
    }

    @Operation(summary = "Baixar XML do documento de distribuição NF-e por ID")
    @GetMapping(value = "/received/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> baixarXmlRecebida(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        byte[] xml = nuvemFiscalService.baixarXmlDocumentoDistribuicaoNfe(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        String disposition = download ? "attachment" : "inline";
        headers.setContentDispositionFormData(disposition, "nfe-recebida-" + id + ".xml");
        headers.setContentLength(xml.length);
        return ResponseEntity.ok().headers(headers).body(xml);
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

    @Operation(summary = "Baixar XML da NF-e por ID")
    @GetMapping(value = "/{id}/xml", produces = MediaType.APPLICATION_XML_VALUE)
    public ResponseEntity<byte[]> obterXml(
            @PathVariable String id,
            @RequestParam(required = false, defaultValue = "false") boolean download) {
        byte[] xml = nuvemFiscalService.baixarXmlNfe(id);
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_XML);
        String disposition = download ? "attachment" : "inline";
        headers.setContentDispositionFormData(disposition, "nfe-" + id + ".xml");
        headers.setContentLength(xml.length);
        return ResponseEntity.ok().headers(headers).body(xml);
    }
}
