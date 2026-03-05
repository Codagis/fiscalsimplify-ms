package com.fiscalimplify.fiscalimplify.integration.nuvemfiscal;

import com.fiscalimplify.fiscalimplify.dto.NfcConfigRequest;
import com.fiscalimplify.fiscalimplify.dto.NfeConfigRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

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
    private static final String AMBIENTE_HOMOLOGACAO = "homologacao";
    private static final int CRT_PADRAO = 3;
    private static final int ID_CSC_PADRAO = 0;

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
