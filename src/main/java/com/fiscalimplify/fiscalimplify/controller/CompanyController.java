package com.fiscalimplify.fiscalimplify.controller;

import com.fiscalimplify.fiscalimplify.documentation.DefaultApiResponses;
import com.fiscalimplify.fiscalimplify.domain.entity.Company;
import com.fiscalimplify.fiscalimplify.domain.repository.CompanyRepository;
import com.fiscalimplify.fiscalimplify.dto.CertificadoRequest;
import com.fiscalimplify.fiscalimplify.dto.CompanyRequest;
import com.fiscalimplify.fiscalimplify.dto.CompanyUpdateRequest;
import com.fiscalimplify.fiscalimplify.dto.DistNfeConfigRequest;
import com.fiscalimplify.fiscalimplify.dto.NfcConfigRequest;
import com.fiscalimplify.fiscalimplify.dto.NfeConfigRequest;
import com.fiscalimplify.fiscalimplify.exception.RegraNegocioException;
import com.fiscalimplify.fiscalimplify.integration.nuvemfiscal.NuvemFiscalService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.transaction.annotation.Transactional;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller de empresas na API Fiscalimplify e Nuvem Fiscal.
 *
 * @author VendaLume
 * @version 1.0.0
 * @since 2025-02-16
 */
@Tag(name = "Empresas", description = "Cadastro de empresas e configuração fiscal (Nuvem Fiscal)")
@DefaultApiResponses
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyRepository repository;
    private final NuvemFiscalService nuvemFiscalService;

    @Operation(summary = "Criar empresa")
    @PostMapping
    @Transactional
    public ResponseEntity<Company> criar(@Valid @RequestBody CompanyRequest request) {
        if (repository.existsByCnpj(request.getCnpj())) {
            throw new RegraNegocioException("Já existe empresa cadastrada com este CNPJ");
        }

        Company company = new Company();
        company.setCnpj(request.getCnpj());
        company.setRazaoSocial(request.getRazaoSocial());
        company.setNomeFantasia(request.getNomeFantasia());
        company.setInscricaoEstadual(request.getInscricaoEstadual());
        company.setUf(request.getUf());
        company.setCodigoMunicipio(request.getCodigoMunicipio());
        company.setNomeMunicipio(request.getNomeMunicipio());
        company.setCrt(request.getCrt());

        Company saved = repository.save(company);

        try {
            nuvemFiscalService.cadastrarEmpresa(
                    saved.getCnpj(),
                    saved.getRazaoSocial(),
                    saved.getNomeFantasia(),
                    saved.getInscricaoEstadual(),
                    saved.getUf(),
                    saved.getCodigoMunicipio(),
                    saved.getNomeMunicipio()
            );
            saved.setRegistradaNuvem(true);
            repository.save(saved);
        } catch (Exception e) {
            log.error("Falha ao cadastrar empresa na Nuvem Fiscal. Empresa salva localmente.", e);
            throw new RegraNegocioException("Empresa salva, mas falha ao registrar na Nuvem Fiscal: " + e.getMessage(), e);
        }

        log.info("Empresa criada: {} - {}", saved.getId(), saved.getRazaoSocial());
        return ResponseEntity.status(HttpStatus.CREATED).body(saved);
    }

    @Operation(summary = "Listar empresas")
    @GetMapping
    public ResponseEntity<List<Company>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }


    @Operation(summary = "Buscar empresa por ID")
    @GetMapping("/{id}")
    public ResponseEntity<Company> buscarPorId(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Buscar empresa por CNPJ")
    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<Company> buscarPorCnpj(@PathVariable String cnpj) {
        return repository.findByCnpj(cnpj.replaceAll("\\D", ""))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @Operation(summary = "Atualizar empresa")
    @PatchMapping("/{cnpj}")
    @Transactional
    public ResponseEntity<Company> atualizar(
            @PathVariable String cnpj,
            @Valid @RequestBody CompanyUpdateRequest request) {
        String cnpjLimpo = cnpj.replaceAll("\\D", "");
        Company company = repository.findByCnpj(cnpjLimpo)
                .orElseThrow(() -> new RegraNegocioException("Empresa não encontrada com CNPJ: " + cnpj));

        if (request.getInscricaoEstadual() != null) {
            company.setInscricaoEstadual(request.getInscricaoEstadual());
        }
        if (request.getUf() != null) {
            company.setUf(request.getUf());
        }
        if (request.getCodigoMunicipio() != null) {
            company.setCodigoMunicipio(request.getCodigoMunicipio());
        }
        if (request.getNomeMunicipio() != null) {
            company.setNomeMunicipio(request.getNomeMunicipio());
        }
        if (request.getCrt() != null) {
            company.setCrt(request.getCrt());
        }

        Company saved = repository.save(company);
        log.info("Empresa atualizada: CNPJ {} - IE {}", cnpjLimpo, request.getInscricaoEstadual());
        return ResponseEntity.ok(saved);
    }

    @Operation(summary = "Cadastrar certificado digital A1 (PFX)")
    @PutMapping("/{cnpj}/certificado")
    public ResponseEntity<Map<String, Object>> cadastrarCertificado(
            @PathVariable String cnpj,
            @Valid @RequestBody CertificadoRequest request) {
        String cnpjLimpo = cnpj.replaceAll("\\D", "");
        Company company = repository.findByCnpj(cnpjLimpo)
                .orElseThrow(() -> new RegraNegocioException("Empresa não encontrada com CNPJ: " + cnpj));

        Map<String, Object> resultado;
        try {
            resultado = nuvemFiscalService.cadastrarCertificado(
                    cnpjLimpo,
                    request.getCertificado(),
                    request.getPassword()
            );
        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("EmpresaNotFound"))) {
                log.info("Empresa CNPJ {} não está na Nuvem Fiscal (404). Cadastrando e tentando novamente...", cnpjLimpo);
                garantirEmpresaNaNuvemFiscal(company);
                resultado = nuvemFiscalService.cadastrarCertificado(
                        cnpjLimpo,
                        request.getCertificado(),
                        request.getPassword()
                );
            } else {
                throw e;
            }
        }

        log.info("Certificado cadastrado para empresa CNPJ {}", cnpjLimpo);
        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Configurar NFC-e na empresa")
    @PutMapping("/{cnpj}/nfce/config")
    public ResponseEntity<Map<String, Object>> configurarNfce(
            @PathVariable String cnpj,
            @Valid @RequestBody NfcConfigRequest request) {
        String cnpjLimpo = cnpj.replaceAll("\\D", "");
        Company company = repository.findByCnpj(cnpjLimpo)
                .orElseThrow(() -> new RegraNegocioException("Empresa não encontrada com CNPJ: " + cnpj));

        Map<String, Object> resultado;
        try {
            resultado = nuvemFiscalService.configurarNfce(cnpjLimpo, request);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("EmpresaNotFound"))) {
                log.info("Empresa CNPJ {} não está na Nuvem Fiscal (404). Cadastrando e tentando novamente...", cnpjLimpo);
                garantirEmpresaNaNuvemFiscal(company);
                resultado = nuvemFiscalService.configurarNfce(cnpjLimpo, request);
            } else {
                throw e;
            }
        }

        log.info("NFC-e configurada para empresa CNPJ {}", cnpjLimpo);
        return ResponseEntity.ok(resultado);
    }

    private void garantirEmpresaNaNuvemFiscal(Company company) {
        String ie = company.getInscricaoEstadual() != null && !company.getInscricaoEstadual().isBlank()
                ? company.getInscricaoEstadual() : "ISENTO";
        nuvemFiscalService.cadastrarEmpresa(
                company.getCnpj(),
                company.getRazaoSocial(),
                company.getNomeFantasia(),
                ie,
                company.getUf(),
                company.getCodigoMunicipio(),
                company.getNomeMunicipio()
        );
        company.setRegistradaNuvem(true);
        repository.save(company);
    }

    @Operation(summary = "Configurar NF-e na empresa")
    @PutMapping("/{cnpj}/nfe/config")
    public ResponseEntity<Map<String, Object>> configurarNfe(
            @PathVariable String cnpj,
            @Valid @RequestBody NfeConfigRequest request) {
        String cnpjLimpo = cnpj.replaceAll("\\D", "");
        if (!repository.existsByCnpj(cnpjLimpo)) {
            throw new RegraNegocioException("Empresa não encontrada com CNPJ: " + cnpj);
        }

        Map<String, Object> resultado = nuvemFiscalService.configurarNfe(cnpjLimpo, request);

        log.info("NF-e configurada para empresa CNPJ {}", cnpjLimpo);
        return ResponseEntity.ok(resultado);
    }

    @Operation(summary = "Configurar Distribuição NF-e (notas recebidas / DF-e)")
    @PutMapping("/{cnpj}/distnfe/config")
    public ResponseEntity<Map<String, Object>> configurarDistNfe(
            @PathVariable String cnpj,
            @Valid @RequestBody DistNfeConfigRequest request) {
        String cnpjLimpo = cnpj.replaceAll("\\D", "");
        Company company = repository.findByCnpj(cnpjLimpo)
                .orElseThrow(() -> new RegraNegocioException("Empresa não encontrada com CNPJ: " + cnpj));

        Map<String, Object> resultado;
        try {
            resultado = nuvemFiscalService.configurarDistNfe(cnpjLimpo, request);
        } catch (RuntimeException e) {
            if (e.getMessage() != null && (e.getMessage().contains("404") || e.getMessage().contains("EmpresaNotFound"))) {
                log.info("Empresa CNPJ {} não está na Nuvem Fiscal (404). Cadastrando e tentando novamente...", cnpjLimpo);
                garantirEmpresaNaNuvemFiscal(company);
                resultado = nuvemFiscalService.configurarDistNfe(cnpjLimpo, request);
            } else {
                throw e;
            }
        }

        log.info("Distribuição NF-e configurada para empresa CNPJ {}", cnpjLimpo);
        return ResponseEntity.ok(resultado);
    }

}
