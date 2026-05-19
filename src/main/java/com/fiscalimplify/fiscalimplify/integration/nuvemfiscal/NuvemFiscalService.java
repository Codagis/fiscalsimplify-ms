package com.fiscalimplify.fiscalimplify.integration.nuvemfiscal;

import com.fiscalimplify.fiscalimplify.dto.DistNfeConfigRequest;
import com.fiscalimplify.fiscalimplify.dto.NfcConfigRequest;
import com.fiscalimplify.fiscalimplify.dto.NfeConfigRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriBuilder;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Serviço responsável pela integração com a API Nuvem Fiscal.
 * Cadastra empresas, certificados e configura NF-e e NFC-e.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class NuvemFiscalService {

    private static final String URI_EMPRESAS = "/empresas";
    private static final String URI_CERTIFICADO = "/empresas/{cpf_cnpj}/certificado";
    private static final String URI_NFCE = "/empresas/{cpf_cnpj}/nfce";
    private static final String URI_NFE = "/empresas/{cpf_cnpj}/nfe";
    private static final String URI_DIST_NFE_CONFIG = "/empresas/{cpf_cnpj}/distnfe";
    private static final String AMBIENTE_HOMOLOGACAO = "homologacao";
    private static final int CRT_PADRAO = 3;
    private static final int ID_CSC_PADRAO = 0;

    private static final String URI_LISTAR_NFE = "/nfe";
    private static final String URI_LISTAR_NFCE = "/nfce";
    private static final String URI_NFE_BY_ID = "/nfe/{id}";
    private static final String URI_NFE_XML = "/nfe/{id}/xml";
    private static final String URI_NFCE_BY_ID = "/nfce/{id}";
    private static final String URI_NFCE_XML = "/nfce/{id}/xml";
    private static final String URI_DIST_NFE_SOLICITAR = "/distribuicao/nfe";
    private static final String URI_DIST_NFE_DOCUMENTOS = "/distribuicao/nfe/documentos";
    private static final String URI_DIST_NFE_DOCUMENTO_BY_ID = "/distribuicao/nfe/documentos/{id}";
    private static final String URI_DIST_NFE_DOCUMENTO_PDF = "/distribuicao/nfe/documentos/{id}/pdf";
    private static final String URI_DIST_NFE_DOCUMENTO_XML = "/distribuicao/nfe/documentos/{id}/xml";

    @Qualifier("nuvemFiscalClient")
    private final WebClient webClient;

    @Value("${nuvemfiscal.ambiente:homologacao}")
    private String ambiente;

    public Map<String, Object> cadastrarEmpresa(String cnpj, String razaoSocial, String nomeFantasia, String inscricaoEstadual,
                                                String uf, String codigoMunicipio, String nomeMunicipio) {
        log.info("Cadastrando empresa na Nuvem Fiscal: CNPJ {} - UF {} - Municipio {}", cnpj, uf, nomeMunicipio);

        Map<String, Object> body = montarBodyEmpresa(cnpj, razaoSocial, nomeFantasia, inscricaoEstadual, uf, codigoMunicipio, nomeMunicipio);

        return executarRequest(
                () -> webClient.post().uri(URI_EMPRESAS).bodyValue(body),
                () -> log.debug("Empresa cadastrada na Nuvem Fiscal")
        );
    }

    public Map<String, Object> cadastrarCertificado(String cnpj, String certificadoBase64, String password) {
        log.info("Cadastrando certificado na Nuvem Fiscal: CNPJ {}", cnpj);

        Map<String, Object> body = Map.of(
                "certificado", certificadoBase64,
                "password", password
        );

        String cnpjLimpo = limparCnpj(cnpj);

        return executarRequest(
                () -> webClient.put().uri(URI_CERTIFICADO, cnpjLimpo).bodyValue(body),
                () -> log.info("Certificado cadastrado com sucesso: CNPJ {}", cnpjLimpo)
        );
    }

    public Map<String, Object> configurarNfce(String cnpj, NfcConfigRequest request) {
        log.info("Configurando NFC-e na Nuvem Fiscal: CNPJ {}", cnpj);

        Map<String, Object> body = montarBodyConfigNfce(request);
        String cnpjLimpo = limparCnpj(cnpj);

        return executarRequest(
                () -> webClient.put().uri(URI_NFCE, cnpjLimpo).bodyValue(body),
                () -> log.info("NFC-e configurada com sucesso: CNPJ {}", cnpjLimpo)
        );
    }

    public Map<String, Object> configurarNfe(String cnpj, NfeConfigRequest request) {
        log.info("Configurando NF-e na Nuvem Fiscal: CNPJ {}", cnpj);

        Map<String, Object> body = montarBodyConfigNfe(request);
        String cnpjLimpo = limparCnpj(cnpj);

        return executarRequest(
                () -> webClient.put().uri(URI_NFE, cnpjLimpo).bodyValue(body),
                () -> log.info("NF-e configurada com sucesso: CNPJ {}", cnpjLimpo)
        );
    }

    /**
     * Configura Distribuição NF-e (DF-e) na Nuvem Fiscal — obrigatório antes de buscar notas recebidas na SEFAZ.
     */
    public Map<String, Object> configurarDistNfe(String cnpj, DistNfeConfigRequest request) {
        log.info("Configurando Distribuição NF-e na Nuvem Fiscal: CNPJ {}", cnpj);

        Map<String, Object> body = montarBodyConfigDistNfe(request);
        String cnpjLimpo = limparCnpj(cnpj);

        return executarRequest(
                () -> webClient.put().uri(URI_DIST_NFE_CONFIG, cnpjLimpo).bodyValue(body),
                () -> log.info("Distribuição NF-e configurada com sucesso: CNPJ {}", cnpjLimpo)
        );
    }

    public void garantirConfigDistribuicaoNfe(String cnpj, String ambiente) {
        String amb = (ambiente != null && ambiente.equalsIgnoreCase("producao")) ? "producao" : AMBIENTE_HOMOLOGACAO;
        DistNfeConfigRequest request = new DistNfeConfigRequest();
        request.setAmbiente(amb);
        configurarDistNfe(cnpj, request);
    }

    /**
     * Lista NF-e emitidas pela empresa (emitente).
     * Mapeia diretamente o endpoint GET /nfe da Nuvem Fiscal.
     */
    public Map<String, Object> listarNfeEmitidas(
            String cnpjEmitente,
            String ambiente,
            Integer top,
            Integer skip,
            Boolean inlinecount,
            String referencia,
            String chave,
            String serie
    ) {
        String cnpjLimpo = limparCnpj(cnpjEmitente);
        String amb = (ambiente != null && ambiente.equalsIgnoreCase("producao")) ? "producao" : AMBIENTE_HOMOLOGACAO;
        return executarRequest(
                () -> webClient.get().uri(uriBuilder -> montarListagemBase(uriBuilder, URI_LISTAR_NFE, top, skip, inlinecount)
                        .queryParam("cpf_cnpj", cnpjLimpo)
                        .queryParam("ambiente", amb)
                        .queryParamIfPresent("referencia", Optional.ofNullable(referencia).filter(s -> !s.isBlank()))
                        .queryParamIfPresent("chave", Optional.ofNullable(chave).filter(s -> !s.isBlank()))
                        .queryParamIfPresent("serie", Optional.ofNullable(serie).filter(s -> !s.isBlank()))
                        .build()),
                () -> log.debug("NF-e listadas (emitidas) para CNPJ {}", cnpjLimpo)
        );
    }

    /**
     * Lista NFC-e emitidas pela empresa (emitente).
     * Mapeia diretamente o endpoint GET /nfce da Nuvem Fiscal.
     */
    public Map<String, Object> listarNfceEmitidas(
            String cnpjEmitente,
            String ambiente,
            Integer top,
            Integer skip,
            Boolean inlinecount,
            String referencia,
            String chave,
            String serie
    ) {
        String cnpjLimpo = limparCnpj(cnpjEmitente);
        String amb = (ambiente != null && ambiente.equalsIgnoreCase("producao")) ? "producao" : AMBIENTE_HOMOLOGACAO;
        return executarRequest(
                () -> webClient.get().uri(uriBuilder -> montarListagemBase(uriBuilder, URI_LISTAR_NFCE, top, skip, inlinecount)
                        .queryParam("cpf_cnpj", cnpjLimpo)
                        .queryParam("ambiente", amb)
                        .queryParamIfPresent("referencia", Optional.ofNullable(referencia).filter(s -> !s.isBlank()))
                        .queryParamIfPresent("chave", Optional.ofNullable(chave).filter(s -> !s.isBlank()))
                        .queryParamIfPresent("serie", Optional.ofNullable(serie).filter(s -> !s.isBlank()))
                        .build()),
                () -> log.debug("NFC-e listadas (emitidas) para CNPJ {}", cnpjLimpo)
        );
    }

    public Map<String, Object> buscarNfePorId(String id) {
        if (id == null || id.isBlank()) return Map.of();
        return executarRequest(
                () -> webClient.get().uri(URI_NFE_BY_ID, id),
                () -> log.debug("NF-e detalhada: {}", id)
        );
    }

    public byte[] baixarXmlNfe(String id) {
        if (id == null || id.isBlank()) return new byte[0];
        return webClient.get()
                .uri(URI_NFE_XML, id)
                .accept(org.springframework.http.MediaType.APPLICATION_XML)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Nuvem Fiscal: " + resp.statusCode() + " - " + body))
                )
                .bodyToMono(byte[].class)
                .blockOptional()
                .orElse(new byte[0]);
    }

    public Map<String, Object> buscarNfcePorId(String id) {
        if (id == null || id.isBlank()) return Map.of();
        return executarRequest(
                () -> webClient.get().uri(URI_NFCE_BY_ID, id),
                () -> log.debug("NFC-e detalhada: {}", id)
        );
    }

    public byte[] baixarXmlNfce(String id) {
        if (id == null || id.isBlank()) return new byte[0];
        return webClient.get()
                .uri(URI_NFCE_XML, id)
                .accept(org.springframework.http.MediaType.APPLICATION_XML)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Nuvem Fiscal: " + resp.statusCode() + " - " + body))
                )
                .bodyToMono(byte[].class)
                .blockOptional()
                .orElse(new byte[0]);
    }

    /**
     * Solicita à SEFAZ novos documentos de distribuição NF-e (DF-e) para o CNPJ interessado (destinatário).
     */
    public Map<String, Object> solicitarDistribuicaoNfe(
            String cnpjInteressado,
            String ambiente,
            String ufAutor,
            Integer distNsu
    ) {
        String cnpjLimpo = limparCnpj(cnpjInteressado);
        String amb = (ambiente != null && ambiente.equalsIgnoreCase("producao")) ? "producao" : AMBIENTE_HOMOLOGACAO;
        String uf = (ufAutor != null && !ufAutor.isBlank()) ? ufAutor.trim().toUpperCase() : "SP";

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cpf_cnpj", cnpjLimpo);
        body.put("ambiente", amb);
        body.put("uf_autor", uf);
        body.put("tipo_consulta", "dist-nsu");
        // false = respeita intervalo mínimo de 1h quando não há novos documentos (evita "Consumo Indevido")
        body.put("ignorar_tempo_espera", false);
        // Obrigatório para dist-nsu: 0 = primeira consulta / desde o início
        body.put("dist_nsu", distNsu != null ? distNsu : 0);

        log.info("NuvemFiscal: POST {} cpf_cnpj={} ambiente={} uf_autor={}", URI_DIST_NFE_SOLICITAR, cnpjLimpo, amb, uf);
        try {
            return executarRequest(
                    () -> webClient.post().uri(URI_DIST_NFE_SOLICITAR).bodyValue(body),
                    () -> log.debug("Distribuição NF-e solicitada para CNPJ {}", cnpjLimpo)
            );
        } catch (RuntimeException e) {
            if (isConfigDistNfeNotFound(e)) {
                log.info("Configuração de Distribuição NF-e ausente para CNPJ {}. Configurando e tentando novamente...", cnpjLimpo);
                garantirConfigDistribuicaoNfe(cnpjLimpo, amb);
                return executarRequest(
                        () -> webClient.post().uri(URI_DIST_NFE_SOLICITAR).bodyValue(body),
                        () -> log.debug("Distribuição NF-e solicitada para CNPJ {} (após configurar distnfe)", cnpjLimpo)
                );
            }
            throw e;
        }
    }

    /**
     * Lista documentos NF-e distribuídos para a empresa interessada (tipicamente notas recebidas).
     * Isso atende o cenário de "notas a pagar" (empresa como destinatária/interessada).
     */
    public Map<String, Object> listarNfeRecebidasDistribuicao(
            String cnpjInteressado,
            String ambiente,
            Integer top,
            Integer skip,
            Boolean inlinecount,
            Integer distNsu,
            String formaDistribuicao,
            String chaveAcesso
    ) {
        String cnpjLimpo = limparCnpj(cnpjInteressado);
        String amb = (ambiente != null && ambiente.equalsIgnoreCase("producao")) ? "producao" : AMBIENTE_HOMOLOGACAO;
        log.info("NuvemFiscal: GET {} cpf_cnpj={} ambiente={} top={} skip={}",
                URI_DIST_NFE_DOCUMENTOS, cnpjLimpo, amb, top, skip);
        try {
            return executarRequest(
                    () -> webClient.get().uri(uriBuilder -> montarListagemBase(uriBuilder, URI_DIST_NFE_DOCUMENTOS, top, skip, inlinecount)
                            .queryParam("cpf_cnpj", cnpjLimpo)
                            .queryParam("ambiente", amb)
                            .queryParamIfPresent("dist_nsu", Optional.ofNullable(distNsu))
                            .queryParam("tipo_documento", "nota")
                            .queryParamIfPresent("forma_distribuicao", Optional.ofNullable(formaDistribuicao).filter(s -> !s.isBlank()))
                            .queryParamIfPresent("chave_acesso", Optional.ofNullable(chaveAcesso).filter(s -> !s.isBlank()))
                            .build()),
                    () -> log.debug("Distribuição NF-e listada para CNPJ {}", cnpjLimpo)
            );
        } catch (RuntimeException e) {
            if (isConfigDistNfeNotFound(e)) {
                log.info("Configuração de Distribuição NF-e ausente para CNPJ {}. Configurando e tentando listar novamente...", cnpjLimpo);
                garantirConfigDistribuicaoNfe(cnpjLimpo, amb);
                return executarRequest(
                        () -> webClient.get().uri(uriBuilder -> montarListagemBase(uriBuilder, URI_DIST_NFE_DOCUMENTOS, top, skip, inlinecount)
                                .queryParam("cpf_cnpj", cnpjLimpo)
                                .queryParam("ambiente", amb)
                                .queryParamIfPresent("dist_nsu", Optional.ofNullable(distNsu))
                                .queryParam("tipo_documento", "nota")
                                .queryParamIfPresent("forma_distribuicao", Optional.ofNullable(formaDistribuicao).filter(s -> !s.isBlank()))
                                .queryParamIfPresent("chave_acesso", Optional.ofNullable(chaveAcesso).filter(s -> !s.isBlank()))
                                .build()),
                        () -> log.debug("Distribuição NF-e listada para CNPJ {} (após configurar distnfe)", cnpjLimpo)
                );
            }
            // Degrada com elegância quando o client não tem scope distnfe habilitado
            Throwable cause = e.getCause();
            String msg = e.getMessage() != null ? e.getMessage() : "";
            if ((cause instanceof WebClientResponseException.Forbidden) || msg.contains("403 FORBIDDEN")) {
                if (msg.contains("distnfe") || msg.contains("distribuicao-nfe")
                        || (cause != null && cause.getMessage() != null
                        && (cause.getMessage().contains("distnfe") || cause.getMessage().contains("distribuicao-nfe")))) {
                    log.warn("Nuvem Fiscal: client sem scope de distribuição NF-e. Retornando lista vazia para distribuição NF-e.");
                    Map<String, Object> out = new LinkedHashMap<>();
                    out.put("data", java.util.List.of());
                    out.put("warning", "Client OAuth não possui o scope de distribuição NF-e habilitado na Nuvem Fiscal (ex.: 'distribuicao-nfe'). Habilite o escopo para consultar notas recebidas.");
                    if (Boolean.TRUE.equals(inlinecount)) out.put("@count", 0);
                    return out;
                }
            }
            throw e;
        }
    }

    public Map<String, Object> buscarDocumentoDistribuicaoNfe(String id) {
        if (id == null || id.isBlank()) return Map.of();
        return executarRequest(
                () -> webClient.get().uri(URI_DIST_NFE_DOCUMENTO_BY_ID, id),
                () -> log.debug("Documento distribuição NF-e detalhado: {}", id)
        );
    }

    public byte[] baixarPdfDocumentoDistribuicaoNfe(String id) {
        if (id == null || id.isBlank()) return new byte[0];
        return webClient.get()
                .uri(URI_DIST_NFE_DOCUMENTO_PDF, id)
                .accept(org.springframework.http.MediaType.APPLICATION_PDF)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Nuvem Fiscal: " + resp.statusCode() + " - " + body))
                )
                .bodyToMono(byte[].class)
                .blockOptional()
                .orElse(new byte[0]);
    }

    public byte[] baixarXmlDocumentoDistribuicaoNfe(String id) {
        if (id == null || id.isBlank()) return new byte[0];
        return webClient.get()
                .uri(URI_DIST_NFE_DOCUMENTO_XML, id)
                .accept(org.springframework.http.MediaType.APPLICATION_XML)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Nuvem Fiscal: " + resp.statusCode() + " - " + body))
                )
                .bodyToMono(byte[].class)
                .blockOptional()
                .orElse(new byte[0]);
    }

    private UriBuilder montarListagemBase(UriBuilder uriBuilder, String path, Integer top, Integer skip, Boolean inlinecount) {
        UriBuilder b = uriBuilder.path(path);
        if (top != null) b.queryParam("$top", top);
        if (skip != null) b.queryParam("$skip", skip);
        if (inlinecount != null) b.queryParam("$inlinecount", inlinecount);
        return b;
    }

    private Map<String, Object> montarBodyEmpresa(String cnpj, String razaoSocial, String nomeFantasia, String inscricaoEstadual,
                                                   String uf, String codigoMunicipio, String nomeMunicipio) {
        String ufVal = (uf != null && !uf.isBlank()) ? uf.trim().toUpperCase() : "SP";
        String codMun = (codigoMunicipio != null && !codigoMunicipio.isBlank()) ? codigoMunicipio.trim() : "3550308";
        String cidade = (nomeMunicipio != null && !nomeMunicipio.isBlank()) ? nomeMunicipio.trim() : nomeMunicipioPorCodigo(codMun);

        Map<String, Object> endereco = Map.of(
                "logradouro", "Rua Teste",
                "numero", "100",
                "complemento", "",
                "bairro", "Centro",
                "codigo_municipio", codMun,
                "cidade", cidade,
                "uf", ufVal,
                "codigo_pais", "1058",
                "pais", "Brasil",
                "cep", "01001000"
        );

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("cpf_cnpj", cnpj);
        body.put("nome_razao_social", razaoSocial);
        body.put("nome_fantasia", nomeFantasiaOuRazaoSocial(nomeFantasia, razaoSocial));
        body.put("email", "contato@empresa-teste.com.br");
        body.put("endereco", endereco);

        Optional.ofNullable(inscricaoEstadual)
                .filter(ie -> !ie.isBlank())
                .ifPresent(ie -> body.put("inscricao_estadual", ie));

        return body;
    }

    private Map<String, Object> montarBodyConfigNfce(NfcConfigRequest request) {
        Map<String, Object> sefaz = Map.of(
                "id_csc", valorOuPadrao(request.getIdCsc(), ID_CSC_PADRAO),
                "csc", valorOuPadrao(request.getCsc(), "")
        );

        return Map.of(
                "CRT", valorOuPadrao(request.getCrt(), CRT_PADRAO),
                "sefaz", sefaz,
                "ambiente", valorOuPadrao(request.getAmbiente(), AMBIENTE_HOMOLOGACAO)
        );
    }

    private Map<String, Object> montarBodyConfigNfe(NfeConfigRequest request) {
        return Map.of(
                "CRT", valorOuPadrao(request.getCrt(), CRT_PADRAO),
                "ambiente", valorOuPadrao(request.getAmbiente(), AMBIENTE_HOMOLOGACAO)
        );
    }

    private Map<String, Object> montarBodyConfigDistNfe(DistNfeConfigRequest request) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("ambiente", valorOuPadrao(request.getAmbiente(), AMBIENTE_HOMOLOGACAO));
        body.put("distribuicao_automatica", Boolean.TRUE.equals(request.getDistribuicaoAutomatica()));
        body.put("distribuicao_intervalo_horas", valorOuPadrao(request.getDistribuicaoIntervaloHoras(), 24));
        body.put("ciencia_automatica", Boolean.TRUE.equals(request.getCienciaAutomatica()));
        return body;
    }

    private boolean isConfigDistNfeNotFound(RuntimeException e) {
        String msg = e.getMessage();
        return msg != null && msg.contains("ConfigDistNfeNotFound");
    }

    private String nomeFantasiaOuRazaoSocial(String nomeFantasia, String razaoSocial) {
        return (nomeFantasia != null && !nomeFantasia.isBlank()) ? nomeFantasia : razaoSocial;
    }

    private <T> T valorOuPadrao(T valor, T padrao) {
        return valor != null ? valor : padrao;
    }

    private String limparCnpj(String cnpj) {
        return cnpj != null ? cnpj.replaceAll("\\D", "") : "";
    }

    /** Mapeamento código IBGE capital -> nome. Usado quando nomeMunicipio não é informado. */
    private static final Map<String, String> CODIGO_PARA_CIDADE = Map.ofEntries(
            Map.entry("1200403", "Rio Branco"), Map.entry("2704302", "Maceio"), Map.entry("1302603", "Manaus"),
            Map.entry("1600303", "Macapa"), Map.entry("2927408", "Salvador"), Map.entry("2304400", "Fortaleza"),
            Map.entry("5300108", "Brasilia"), Map.entry("3205309", "Vitoria"), Map.entry("5208707", "Goiania"),
            Map.entry("2111300", "Sao Luis"), Map.entry("3106200", "Belo Horizonte"), Map.entry("5002704", "Campo Grande"),
            Map.entry("5103403", "Cuiaba"), Map.entry("1501402", "Belem"), Map.entry("2507507", "Joao Pessoa"),
            Map.entry("2611606", "Recife"), Map.entry("2211001", "Teresina"), Map.entry("4113700", "Curitiba"),
            Map.entry("3304557", "Rio de Janeiro"), Map.entry("2408102", "Natal"), Map.entry("1100205", "Porto Velho"),
            Map.entry("1400100", "Boa Vista"), Map.entry("4314902", "Porto Alegre"), Map.entry("4205407", "Florianopolis"),
            Map.entry("2800308", "Aracaju"), Map.entry("3550308", "Sao Paulo"), Map.entry("1721000", "Palmas")
    );

    private String nomeMunicipioPorCodigo(String codigoMunicipio) {
        return codigoMunicipio != null && CODIGO_PARA_CIDADE.containsKey(codigoMunicipio)
                ? CODIGO_PARA_CIDADE.get(codigoMunicipio)
                : "Municipio";
    }

    private Map<String, Object> executarRequest(
            Supplier<WebClient.RequestHeadersSpec<?>> requestBuilder,
            Runnable onSuccess) {

        Map<String, Object> result = requestBuilder.get()
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .map(body -> new RuntimeException("Nuvem Fiscal: " + resp.statusCode() + " - " + body))
                )
                .bodyToMono(Map.class)
                .doOnSuccess(r -> onSuccess.run())
                .block();

        return result != null ? result : Map.of();
    }
}
