package com.fiscalimplify.fiscalimplify.controller;

import com.fiscalimplify.fiscalimplify.domain.entity.Company;
import com.fiscalimplify.fiscalimplify.domain.repository.CompanyRepository;
import com.fiscalimplify.fiscalimplify.dto.CertificadoRequest;
import com.fiscalimplify.fiscalimplify.dto.CompanyRequest;
import com.fiscalimplify.fiscalimplify.dto.CompanyUpdateRequest;
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
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Controller REST responsável por cadastro, listagem, atualização e configuração de empresas
 * na API Fiscalimplify e na Nuvem Fiscal (certificado, NF-e e NFC-e).
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@RestController
@RequestMapping("/companies")
@RequiredArgsConstructor
@Slf4j
public class CompanyController {

    private final CompanyRepository repository;
    private final NuvemFiscalService nuvemFiscalService;

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

    @GetMapping
    public ResponseEntity<List<Company>> listar() {
        return ResponseEntity.ok(repository.findAll());
    }

    @GetMapping("/{id}")
    public ResponseEntity<Company> buscarPorId(@PathVariable UUID id) {
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/cnpj/{cnpj}")
    public ResponseEntity<Company> buscarPorCnpj(@PathVariable String cnpj) {
        return repository.findByCnpj(cnpj.replaceAll("\\D", ""))
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

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
}
