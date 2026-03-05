package com.fiscalimplify.fiscalimplify.service;

import com.fiscalimplify.fiscalimplify.domain.repository.CompanyRepository;
import com.fiscalimplify.fiscalimplify.enums.UnidadeFederativa;
import com.fiscalimplify.fiscalimplify.dto.DestinatarioRequest;
import com.fiscalimplify.fiscalimplify.dto.ItemRequest;
import com.fiscalimplify.fiscalimplify.dto.NfceRequest;
import com.fiscalimplify.fiscalimplify.dto.NfeRequest;
import com.fiscalimplify.fiscalimplify.dto.PagamentoRequest;
import com.fiscalimplify.fiscalimplify.exception.RegraNegocioException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Serviço responsável pela emissão de NF-e e NFC-e via API Nuvem Fiscal.
 * Constrói o payload infNFe no formato SEFAZ (ide, emit, dest, det, total, transp, pag).
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FiscalService {

    private static final String URI_NFCE = "/nfce";
    private static final String URI_NFE = "/nfe";
    private static final String URI_NFCE_PDF = "/nfce/{id}/pdf";
    private static final String URI_NFE_PDF = "/nfe/{id}/pdf";
    private static final String VERSAO_NFE = "4.00";
    private static final int MOD_NFCE = 65;
    private static final int MOD_NFE = 55;
    private static final int TP_IMP_NFCE = 4;
    private static final int TP_IMP_NFE = 1;
    private static final int MOD_FRETE_SEM_FRETE = 9;
    private static final int CRT_PADRAO = 3;
    private static final int UF_PADRAO = 35;
    private static final String IE_ISENTO = "ISENTO";
    private static final String CNPJ_TESTE_HOMOLOG = "00000000000191";
    /** CPF para consumidor final não identificado (obrigatório no schema dest) */
    private static final String CPF_CONSUMIDOR_FINAL = "00000000191";
    private static final String NAT_OP_PADRAO = "VENDA";
    private static final String VER_PROC = "Fiscalimplify-1.0";

    @Qualifier("nuvemFiscalClient")
    private final WebClient webClient;

    private final CompanyRepository companyRepository;

    @Value("${nuvemfiscal.ambiente:homologacao}")
    private String ambiente;

    public Map<String, Object> emitirNfce(NfceRequest request) {
        log.info("Emitindo NFC-e via Nuvem Fiscal: CNPJ {}", request.getCnpjEmitente());

        Map<String, Object> infNFe = montarInfNFeNfce(request);
        if (log.isDebugEnabled() && infNFe.containsKey("pag")) {
            log.debug("NFC-e bloco pag enviado à Nuvem Fiscal: {}", infNFe.get("pag"));
        }

        Map<String, Object> body = Map.of(
                "ambiente", ambiente,
                "infNFe", infNFe
        );

        return executarPostJson(URI_NFCE, body, () -> log.debug("NFC-e enviada com sucesso"));
    }

    public Map<String, Object> emitirNfe(NfeRequest request) {
        log.info("Emitindo NF-e via Nuvem Fiscal: CNPJ {}", request.getCnpjEmitente());

        Map<String, Object> body = Map.of(
                "ambiente", ambiente,
                "infNFe", montarInfNFeNfe(request)
        );

        return executarPostJson(URI_NFE, body, () -> log.debug("NF-e enviada com sucesso"));
    }

    /**
     * Busca o PDF da NFC-e na Nuvem Fiscal.
     * Faz retry com delay quando o XML ainda não está disponível (EventoDfeXmlNotFound),
     * pois a geração do PDF pode levar alguns segundos após a emissão.
     * Se após todas as tentativas o PDF não estiver disponível, lança RegraNegocioException
     * para retornar 422 com mensagem amigável em vez de 500.
     */
    public byte[] buscarPdfNfce(String id) {
        log.info("Buscando PDF da NFC-e: {}", id);
        int maxTentativas = 8;
        int delayMs = 3000;

        for (int t = 0; t < maxTentativas; t++) {
            try {
                return executarGetPdf(URI_NFCE_PDF, id);
            } catch (RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean xmlIndisponivel = msg.contains("EventoDfeXmlNotFound") || msg.contains("Xml não disponível") || msg.contains("404");
                if (xmlIndisponivel && t < maxTentativas - 1) {
                    log.info("PDF da NFC-e {} ainda não disponível. Aguardando {} ms antes da tentativa {}/{}.",
                            id, delayMs, t + 2, maxTentativas);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RegraNegocioException("Interrompido ao aguardar geração do PDF.", ie);
                    }
                } else if (xmlIndisponivel) {
                    throw new RegraNegocioException(
                            "O PDF da NFC-e ainda não está disponível na Nuvem Fiscal. A geração pode levar alguns minutos. Tente gerar o cupom fiscal novamente em instantes.");
                } else {
                    throw e;
                }
            }
        }
        throw new RegraNegocioException("PDF da NFC-e não disponível após " + maxTentativas + " tentativas. Tente novamente em alguns instantes.");
    }

    /**
     * Busca o PDF da NF-e na Nuvem Fiscal.
     * Faz retry com delay quando o XML ainda não está disponível (EventoDfeXmlNotFound),
     * pois a geração do PDF pode levar alguns segundos após a emissão.
     */
    public byte[] buscarPdfNfe(String id) {
        log.info("Buscando PDF da NF-e: {}", id);
        int maxTentativas = 8;
        int delayMs = 3000;

        for (int t = 0; t < maxTentativas; t++) {
            try {
                return executarGetPdf(URI_NFE_PDF, id);
            } catch (RuntimeException e) {
                String msg = e.getMessage() != null ? e.getMessage() : "";
                boolean xmlIndisponivel = msg.contains("EventoDfeXmlNotFound") || msg.contains("Xml não disponível") || msg.contains("404");
                if (xmlIndisponivel && t < maxTentativas - 1) {
                    log.info("PDF da NF-e {} ainda não disponível. Aguardando {} ms antes da tentativa {}/{}.",
                            id, delayMs, t + 2, maxTentativas);
                    try {
                        Thread.sleep(delayMs);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw new RegraNegocioException("Interrompido ao aguardar geração do PDF.", ie);
                    }
                } else if (xmlIndisponivel) {
                    throw new RegraNegocioException(
                            "O PDF da NF-e ainda não está disponível na Nuvem Fiscal. A geração pode levar alguns minutos. Tente gerar a NF-e novamente em instantes.");
                } else {
                    throw e;
                }
            }
        }
        throw new RegraNegocioException("PDF da NF-e não disponível após " + maxTentativas + " tentativas. Tente novamente em alguns instantes.");
    }

    private Map<String, Object> executarPostJson(String uri, Map<String, Object> body, Runnable onSuccess) {
        Map<String, Object> result = webClient.post()
                .uri(uri)
                .bodyValue(body)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .map(b -> new RuntimeException("Nuvem Fiscal: " + resp.statusCode() + " - " + b))
                )
                .bodyToMono(Map.class)
                .doOnSuccess(r -> onSuccess.run())
                .block();

        return result != null ? result : Map.of();
    }

    private byte[] executarGetPdf(String uriTemplate, String id) {
        return webClient.get()
                .uri(uriTemplate, id)
                .accept(MediaType.APPLICATION_PDF)
                .retrieve()
                .onStatus(
                        status -> status.is4xxClientError() || status.is5xxServerError(),
                        resp -> resp.bodyToMono(String.class)
                                .map(b -> new RuntimeException("Nuvem Fiscal: " + resp.statusCode() + " - " + b))
                )
                .bodyToMono(byte[].class)
                .block();
    }

    private Map<String, Object> montarInfNFeNfce(NfceRequest request) {
        BigDecimal vProd = calcularValorTotalItens(request.getItens());
        BigDecimal totalPago = request.getPagamentos().stream()
                .map(PagamentoRequest::getValor)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // SEFAZ exige: vNF = vProd - vDesc (+ vFrete etc). vNF é o total comercial da NF, não o valor efetivamente pago.
        // Se o cliente paga menos (desconto): vDesc = vProd - totalPago, vNF = totalPago.
        // Se o cliente paga mais (juros/taxa): vDesc = 0, vNF = vProd (o acréscimo não compõe vNF).
        BigDecimal vDesc = vProd.compareTo(totalPago) > 0 ? vProd.subtract(totalPago) : BigDecimal.ZERO;
        BigDecimal vNF = vProd.subtract(vDesc);

        UfMun ufMun = obterUfEMunicipio(request.getCnpjEmitente());
        int crt = obterCrt(request.getCnpjEmitente());

        Map<String, Object> infNFe = new LinkedHashMap<>();
        infNFe.put("versao", VERSAO_NFE);
        infNFe.put("ide", montarIde(MOD_NFCE, request.getSerie(), request.getNaturezaOperacao(), TP_IMP_NFCE, ufMun.cUF(), ufMun.cMunFG()));
        infNFe.put("emit", montarEmit(request.getCnpjEmitente(), obterIeEmitente(request.getCnpjEmitente(), request.getIeEmitente())));

        Optional.ofNullable(request.getDestinatario()).ifPresent(dest -> {
            boolean temEndereco = dest.getLogradouro() != null && !dest.getLogradouro().isBlank();
            if (temEndereco) {
                infNFe.put("dest", mapearDest(dest));
            } else if (dest.getNome() != null && !dest.getNome().isBlank() && ((dest.getCpf() != null && !dest.getCpf().isBlank()) || (dest.getCnpj() != null && !dest.getCnpj().isBlank()))) {
                infNFe.put("dest", mapearDestNfceConsumidor(dest.getNome(), dest.getCpf(), dest.getCnpj(), ufMun));
            }
        });

        infNFe.put("det", mapearDetComDesconto(request.getItens(), crt, vProd, vDesc));
        infNFe.put("total", Map.of("ICMSTot", montarIcmstot(vProd, vDesc, vNF)));
        infNFe.put("transp", Map.of("modFrete", MOD_FRETE_SEM_FRETE));
        Map<String, Object> pag = new LinkedHashMap<>();
        List<Map<String, Object>> detPagList = mapearDetPag(request.getPagamentos(), vNF);
        pag.put("detPag", detPagList);
        BigDecimal totalPagEnviado = detPagList.stream()
                .map(m -> (BigDecimal) m.get("vPag"))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalPagEnviado.compareTo(vNF) > 0) {
            pag.put("vTroco", totalPagEnviado.subtract(vNF).setScale(2, RoundingMode.HALF_UP));
        }
        infNFe.put("pag", pag);

        return infNFe;
    }

    private Map<String, Object> montarInfNFeNfe(NfeRequest request) {
        BigDecimal vProd = calcularValorTotalItens(request.getItens());
        UfMun ufMun = obterUfEMunicipio(request.getCnpjEmitente());
        int crt = obterCrt(request.getCnpjEmitente());

        Map<String, Object> infNFe = new LinkedHashMap<>();
        infNFe.put("versao", VERSAO_NFE);
        infNFe.put("ide", montarIde(MOD_NFE, request.getSerie(), request.getNaturezaOperacao(), TP_IMP_NFE, ufMun.cUF(), ufMun.cMunFG()));
        infNFe.put("emit", montarEmit(request.getCnpjEmitente(), obterIeEmitente(request.getCnpjEmitente(), request.getIeEmitente())));
        infNFe.put("dest", mapearDest(request.getDestinatario()));
        infNFe.put("det", mapearDet(request.getItens(), crt));
        infNFe.put("total", Map.of("ICMSTot", montarIcmstot(vProd, BigDecimal.ZERO, vProd)));
        infNFe.put("transp", Map.of("modFrete", MOD_FRETE_SEM_FRETE));
        infNFe.put("pag", Map.of("detPag", List.of(Map.of("tPag", "90", "vPag", BigDecimal.ZERO))));

        return infNFe;
    }

    private BigDecimal calcularValorTotalItens(List<ItemRequest> itens) {
        return itens.stream()
                .map(i -> i.getValorUnitario().multiply(i.getQuantidade()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private int obterCrt(String cnpj) {
        String cnpjLimpo = limparCnpj(cnpj);
        return companyRepository.findByCnpj(cnpjLimpo)
                .map(c -> c.getCrt())
                .filter(crt -> crt != null && crt >= 1 && crt <= 4)
                .orElse(CRT_PADRAO);
    }

    private String obterIeEmitente(String cnpj, String ieRequest) {
        if (ieRequest != null && !ieRequest.isBlank()) {
            return ieRequest;
        }
        String cnpjLimpo = limparCnpj(cnpj);
        return companyRepository.findByCnpj(cnpjLimpo)
                .map(c -> c.getInscricaoEstadual())
                .filter(ie -> ie != null && !ie.isBlank())
                .orElse(IE_ISENTO);
    }

    private Map<String, Object> montarEmit(String cnpj, String ie) {
        String ieValor = (ie != null && !ie.isBlank()) ? ie : IE_ISENTO;
        return Map.of("CNPJ", cnpj, "IE", ieValor);
    }

    private record UfMun(int cUF, String cMunFG, String ufSigla) {}

    private UfMun obterUfEMunicipio(String cnpj) {
        String cnpjLimpo = limparCnpj(cnpj);
        var company = companyRepository.findByCnpj(cnpjLimpo)
                .orElseThrow(() -> new RegraNegocioException("Empresa não encontrada: " + cnpj));

        validarUfEMunicipioEmpresa(company.getUf(), company.getCodigoMunicipio(), cnpj);

        UnidadeFederativa uf = UnidadeFederativa.fromSigla(company.getUf());
        int cUF = uf.getCodigoIbge();
        String cMunFG = company.getCodigoMunicipio().trim();

        validarConsistenciaMunicipioUF(cMunFG, cUF, uf.name());

        return new UfMun(cUF, cMunFG, uf.name());
    }

    private void validarUfEMunicipioEmpresa(String uf, String codigoMunicipio, String cnpj) {
        if (uf == null || uf.isBlank() || codigoMunicipio == null || codigoMunicipio.isBlank()) {
            throw new RegraNegocioException(
                    "Empresa sem UF e município cadastrados. Use PATCH /companies/" + cnpj + " com uf (ex: CE) e codigoMunicipio (ex: 2304400 para Fortaleza).");
        }
    }

    private void validarConsistenciaMunicipioUF(String cMunFG, int cUF, String uf) {
        if (cMunFG.length() >= 2) {
            int prefixoMun = Integer.parseInt(cMunFG.substring(0, 2));
            if (prefixoMun != cUF) {
                throw new RegraNegocioException(
                        "Código do município " + cMunFG + " não pertence à UF " + uf + ". O município deve ser do mesmo estado (ex: CE=23, 2304400=Fortaleza).");
            }
        }
    }

    private Map<String, Object> montarIde(int mod, Integer serie, String natOp, int tpImp, int cUF, String cMunFG) {
        String dhEmi = OffsetDateTime.now().format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
        String natOpValor = (natOp != null && !natOp.isBlank()) ? natOp : NAT_OP_PADRAO;
        int serieValor = serie != null ? serie : 1;

        return Map.ofEntries(
                Map.entry("cUF", cUF),
                Map.entry("natOp", natOpValor),
                Map.entry("mod", mod),
                Map.entry("serie", serieValor),
                Map.entry("nNF", (int) (System.currentTimeMillis() % 999999999)),
                Map.entry("dhEmi", dhEmi),
                Map.entry("tpNF", 1),
                Map.entry("idDest", 1),
                Map.entry("cMunFG", cMunFG),
                Map.entry("tpImp", tpImp),
                Map.entry("tpEmis", 1),
                Map.entry("finNFe", 1),
                Map.entry("indFinal", 1),
                Map.entry("indPres", 1),
                Map.entry("procEmi", 0),
                Map.entry("verProc", VER_PROC)
        );
    }

    private List<Map<String, Object>> mapearDet(List<ItemRequest> itens, int crt) {
        return mapearDetComDesconto(itens, crt, null, null);
    }

    /**
     * Monta os itens (det) da NFC-e. Quando vDescTotal > 0, distribui o desconto pelos itens
     * proporcionalmente ao vProd de cada um, para que a SEFAZ valide: "Total do Desconto = somatório dos itens".
     */
    private List<Map<String, Object>> mapearDetComDesconto(List<ItemRequest> itens, int crt, BigDecimal vProdTotal, BigDecimal vDescTotal) {
        boolean simplesNacional = (crt == 1 || crt == 4);
        List<Map<String, Object>> det = new ArrayList<>();
        int n = 1;

        List<BigDecimal> vDescPorItem = distribuirDescontoPorItens(itens, vProdTotal, vDescTotal);

        for (int i = 0; i < itens.size(); i++) {
            ItemRequest item = itens.get(i);
            BigDecimal itemVDesc = (vDescPorItem != null && i < vDescPorItem.size()) ? vDescPorItem.get(i) : BigDecimal.ZERO;
            Map<String, Object> prod = montarProduto(item, n, itemVDesc);
            Map<String, Object> imposto = montarImposto(simplesNacional);
            det.add(Map.of("nItem", n, "prod", prod, "imposto", imposto));
            n++;
        }

        return det;
    }

    private List<BigDecimal> distribuirDescontoPorItens(List<ItemRequest> itens, BigDecimal vProdTotal, BigDecimal vDescTotal) {
        if (vDescTotal == null || vDescTotal.compareTo(BigDecimal.ZERO) <= 0 || vProdTotal == null || vProdTotal.compareTo(BigDecimal.ZERO) <= 0 || itens.isEmpty()) {
            return Collections.emptyList();
        }
        List<BigDecimal> vProds = itens.stream()
                .map(i -> i.getValorUnitario().multiply(i.getQuantidade()))
                .toList();
        List<BigDecimal> result = new ArrayList<>(itens.size());
        BigDecimal acumulado = BigDecimal.ZERO;
        for (int i = 0; i < itens.size(); i++) {
            if (i == itens.size() - 1) {
                result.add(vDescTotal.subtract(acumulado).setScale(2, RoundingMode.HALF_UP));
            } else {
                BigDecimal itemVDesc = vDescTotal.multiply(vProds.get(i)).divide(vProdTotal, 2, RoundingMode.HALF_UP);
                result.add(itemVDesc);
                acumulado = acumulado.add(itemVDesc);
            }
        }
        return result;
    }

    private Map<String, Object> montarProduto(ItemRequest item, int n) {
        return montarProduto(item, n, null);
    }

    private Map<String, Object> montarProduto(ItemRequest item, int n, BigDecimal vDescItem) {
        BigDecimal vProd = item.getValorUnitario().multiply(item.getQuantidade());
        String cProd = (item.getCodigo() != null && !item.getCodigo().isBlank())
                ? item.getCodigo().trim().substring(0, Math.min(60, item.getCodigo().trim().length()))
                : ("ITEM" + n);

        Map<String, Object> prod = new LinkedHashMap<>();
        prod.put("cProd", cProd);
        prod.put("cEAN", "SEM GTIN");
        prod.put("xProd", item.getDescricao());
        prod.put("NCM", item.getNcm());
        prod.put("CFOP", Integer.parseInt(item.getCfop()));
        prod.put("uCom", "UN");
        prod.put("qCom", item.getQuantidade());
        prod.put("vUnCom", item.getValorUnitario());
        prod.put("vProd", vProd);
        if (vDescItem != null && vDescItem.compareTo(BigDecimal.ZERO) > 0) {
            prod.put("vDesc", vDescItem.setScale(2, RoundingMode.HALF_UP));
        }
        prod.put("cEANTrib", "SEM GTIN");
        prod.put("uTrib", "UN");
        prod.put("qTrib", item.getQuantidade());
        prod.put("vUnTrib", item.getValorUnitario());
        prod.put("indTot", 1);

        return prod;
    }

    private Map<String, Object> montarImposto(boolean simplesNacional) {
        if (simplesNacional) {
            return Map.of(
                    "ICMS", Map.of("ICMSSN102", Map.of("orig", 0, "CSOSN", "102")),
                    "PIS", Map.of("PISNT", Map.of("CST", "08")),
                    "COFINS", Map.of("COFINSNT", Map.of("CST", "08"))
            );
        }
        return Map.of(
                "ICMS", Map.of("ICMS00", Map.of("orig", 0, "CST", "00", "modBC", 3, "vBC", 0, "pICMS", 0, "vICMS", 0)),
                "PIS", Map.of("PISNT", Map.of("CST", "08")),
                "COFINS", Map.of("COFINSNT", Map.of("CST", "08"))
        );
    }

    private List<Map<String, Object>> mapearDetPag(List<PagamentoRequest> pagamentos, BigDecimal vNF) {
        boolean unicoPagamentoPix = pagamentos.size() == 1 && "17".equals(pagamentos.get(0).getForma() != null ? pagamentos.get(0).getForma().trim() : "");
        return pagamentos.stream()
                .map(p -> mapearPagamento(p, vNF, unicoPagamentoPix))
                .toList();
    }

    /**
     * Formas de pagamento na NFC-e (modelo 65), alinhado ao MOC e à IN SEFAZ-CE.
     * <p>
     * <b>PIX (tPag = "17")</b><br>
     * Não é obrigatório enviar: CNPJ adquirente, código de autorização, bandeira, NSU, grupo &lt;card&gt;.
     * Esses campos são exigidos apenas para tPag "03" (Cartão de Crédito) e "04" (Cartão de Débito),
     * conforme Manual de Orientação do Contribuinte (MOC) da NFC-e.
     * <p>
     * <b>Ceará (SEFAZ-CE)</b><br>
     * A IN 87/2025 (vinculação de meios de pagamento) dispensa <i>PIX estático</i> e formas que não
     * gerem código de autorização único por transação. Para PIX enviamos apenas tPag e vPag, sem
     * grupo card/adquirente, para não ser interpretado como pagamento eletrônico integrado (cartão).
     * <p>
     * Para PIX: apenas tPag "17" e vPag. Para 03/04: bloco card só quando houver dados completos.
     */
    private Map<String, Object> mapearPagamento(PagamentoRequest p, BigDecimal vNF, boolean unicoPagamentoPix) {
        Map<String, Object> m = new LinkedHashMap<>();
        String forma = (p.getForma() != null) ? p.getForma().trim() : "";
        boolean isPix = "17".equals(forma);
        boolean isCartao = "03".equals(forma) || "04".equals(forma);
        boolean hasCardData = isCartao
                && p.getCnpjAdquirente() != null && !p.getCnpjAdquirente().isBlank()
                && p.getTBand() != null && !p.getTBand().isBlank()
                && p.getCAut() != null && !p.getCAut().isBlank();

        String tPagStr;
        if (isCartao && !hasCardData) {
            tPagStr = "90";
            log.info("NFC-e: forma {} (cartão) sem dados do cartão; enviando tPag=90 (Outros).", forma);
        } else if (isCartao && hasCardData) {
            tPagStr = forma;
        } else {
            tPagStr = forma.isEmpty() ? "99" : forma;
        }

        if (isPix) {
            m.put("tPag", "17");
            m.put("vPag", unicoPagamentoPix && vNF != null ? vNF.setScale(2, RoundingMode.HALF_UP) : (p.getValor() != null ? p.getValor().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO));
            // Estratégia 2 (teste): grupo card com tpIntegra=2 (não integrado) para PIX — alguns estados (ex.: CE) exigem <card> em pagamento eletrônico presencial
            Map<String, Object> cardPix = new LinkedHashMap<>();
            cardPix.put("tpIntegra", 2);
            m.put("card", cardPix);
            return m;
        }

        m.put("indPag", 0);
        m.put("tPag", tPagStr);
        m.put("vPag", p.getValor() != null ? p.getValor().setScale(2, RoundingMode.HALF_UP) : BigDecimal.ZERO);

        if (hasCardData) {
            String cnpj = p.getCnpjAdquirente().replaceAll("\\D", "");
            if (cnpj.length() == 14) {
                Map<String, Object> card = new LinkedHashMap<>();
                card.put("tpIntegra", p.getTpIntegracao() != null ? p.getTpIntegracao() : 2);
                card.put("CNPJ", cnpj);
                card.put("tBand", p.getTBand().trim());
                card.put("cAut", p.getCAut().trim().length() > 20 ? p.getCAut().trim().substring(0, 20) : p.getCAut().trim());
                m.put("card", card);
            }
        }

        if (("03".equals(tPagStr) || "04".equals(tPagStr)) && !m.containsKey("card")) {
            m.put("tPag", "90");
            log.warn("NFC-e: tPag {} sem bloco card; forçando tPag=90.", tPagStr);
        }
        return m;
    }

    private Map<String, Object> montarIcmstot(BigDecimal vProd, BigDecimal vDesc, BigDecimal vNF) {
        return Map.ofEntries(
                Map.entry("vBC", 0),
                Map.entry("vICMS", 0),
                Map.entry("vICMSDeson", 0),
                Map.entry("vFCP", 0),
                Map.entry("vBCST", 0),
                Map.entry("vST", 0),
                Map.entry("vFCPST", 0),
                Map.entry("vFCPSTRet", 0),
                Map.entry("vProd", vProd),
                Map.entry("vFrete", 0),
                Map.entry("vSeg", 0),
                Map.entry("vDesc", vDesc != null ? vDesc : BigDecimal.ZERO),
                Map.entry("vII", 0),
                Map.entry("vIPI", 0),
                Map.entry("vIPIDevol", 0),
                Map.entry("vPIS", 0),
                Map.entry("vCOFINS", 0),
                Map.entry("vOutro", 0),
                Map.entry("vNF", vNF)
        );
    }

    /**
     * Destinatário NFC-e consumidor final: apenas nome e CPF ou CNPJ (sem endereço).
     * SEFAZ aceita endereço genérico para consumidor final.
     */
    private Map<String, Object> mapearDestNfceConsumidor(String nome, String cpf, String cnpj, UfMun ufMun) {
        Map<String, Object> d = new LinkedHashMap<>();
        String nomeValor = (nome != null && !nome.isBlank()) ? nome.trim() : "Consumidor final";
        if (nomeValor.length() > 60) nomeValor = nomeValor.substring(0, 60);
        d.put("xNome", nomeValor);
        String cpfLimpo = cpf != null ? cpf.replaceAll("\\D", "") : null;
        String cnpjLimpo = cnpj != null ? cnpj.replaceAll("\\D", "") : null;
        if (cpfLimpo != null && cpfLimpo.length() == 11) d.put("CPF", cpfLimpo);
        if (cnpjLimpo != null && cnpjLimpo.length() == 14) d.put("CNPJ", cnpjLimpo);
        d.put("indIEDest", 9);
        String uf = ufMun.ufSigla() != null ? ufMun.ufSigla() : "CE";
        String cMun = ufMun.cMunFG() != null && !ufMun.cMunFG().isBlank() ? ufMun.cMunFG() : "2304400";
        d.put("enderDest", Map.of(
                "xLgr", "Não informado",
                "nro", "S/N",
                "xBairro", "Não informado",
                "cMun", cMun,
                "xMun", "Não informado",
                "UF", uf,
                "CEP", "00000000",
                "cPais", "1058",
                "xPais", "Brasil"
        ));
        return d;
    }

    private Map<String, Object> mapearDest(DestinatarioRequest dest) {
        Map<String, Object> d = new LinkedHashMap<>();

        // Schema Nuvem Fiscal exige CNPJ, CPF ou idEstrangeiro como PRIMEIRO elemento em dest
        String cnpj = dest.getCnpj() != null && !dest.getCnpj().isBlank() ? dest.getCnpj().replaceAll("\\D", "") : null;
        String cpf = dest.getCpf() != null && !dest.getCpf().isBlank() ? dest.getCpf().replaceAll("\\D", "") : null;
        if (cnpj != null && cnpj.length() == 14) {
            d.put("CNPJ", cnpj);
        } else if (cpf != null && cpf.length() == 11) {
            d.put("CPF", cpf);
        } else {
            d.put("CPF", CPF_CONSUMIDOR_FINAL);
        }

        d.put("xNome", dest.getNome() != null && !dest.getNome().isBlank() ? dest.getNome() : "Consumidor final");
        int indIEDest = obterIndIeDest(dest);
        d.put("indIEDest", indIEDest);

        Optional.ofNullable(obterIeDestinatario(dest, indIEDest))
                .filter(ie -> ie != null && !ie.isBlank())
                .filter(ie -> ie.matches("[0-9]{2,14}"))
                .ifPresent(ie -> d.put("IE", ie));

        String cMun = obterCodigoMunicipioDestinatario(dest);
        String cep = (dest.getCep() != null && !dest.getCep().isBlank())
                ? dest.getCep().replaceAll("\\D", "")
                : "00000000";
        if (cep.length() != 8) cep = "00000000";
        d.put("enderDest", Map.of(
                "xLgr", dest.getLogradouro(),
                "nro", dest.getNumero(),
                "xBairro", dest.getBairro(),
                "cMun", cMun,
                "xMun", dest.getMunicipio(),
                "UF", dest.getUf(),
                "CEP", cep,
                "cPais", "1058",
                "xPais", "Brasil"
        ));

        return d;
    }

    private String obterCodigoMunicipioDestinatario(DestinatarioRequest dest) {
        if (dest.getCodigoMunicipio() != null && !dest.getCodigoMunicipio().isBlank()) {
            String cMun = dest.getCodigoMunicipio().trim();
            if (cMun.length() == 7 && dest.getUf() != null && validarPrefixoMunicipioUF(cMun, dest.getUf())) {
                return cMun;
            }
        }

        return Optional.ofNullable(dest.getUf())
                .map(UnidadeFederativa::fromSigla)
                .map(UnidadeFederativa::getCodigoMunicipioCapital)
                .orElseThrow(() -> new RegraNegocioException(
                        "Destinatário: informe codigoMunicipio (7 dígitos IBGE) compatível com a UF. Ex: CE=2304400 (Fortaleza), SP=3550308 (São Paulo)."));
    }

    private boolean validarPrefixoMunicipioUF(String cMun, String ufSigla) {
        int prefixo = Integer.parseInt(cMun.substring(0, 2));
        int ufCode = UnidadeFederativa.fromSigla(ufSigla).getCodigoIbge();
        return prefixo == ufCode;
    }

    /**
     * indIEDest: 1=Contribuinte ICMS, 2=Contribuinte isento (CNPJ), 9=Não contribuinte (pessoa física/consumidor).
     * Pessoa física não tem IE; SEFAZ rejeita "isento" para destinatário PF. Usar 9 para CPF ou sem documento.
     */
    private int obterIndIeDest(DestinatarioRequest dest) {
        String cnpj = limparCnpj(dest.getCnpj());
        String cpf = dest.getCpf() != null && !dest.getCpf().isBlank() ? dest.getCpf().replaceAll("\\D", "") : null;
        if (cpf != null && cpf.length() == 11) return 9;
        if (cnpj.isEmpty()) return 9;
        if (CNPJ_TESTE_HOMOLOG.equals(cnpj)) return 2;
        if (dest.getIe() != null && !dest.getIe().isBlank()) return 1;
        return 2;
    }

    private String obterIeDestinatario(DestinatarioRequest dest, int indIEDest) {
        String cnpj = limparCnpj(dest.getCnpj());
        if (CNPJ_TESTE_HOMOLOG.equals(cnpj)) return IE_ISENTO;
        if (indIEDest == 9) return null;
        if (indIEDest == 2) return valorOuPadrao(dest.getIe(), IE_ISENTO);
        return dest.getIe();
    }

    private String valorOuPadrao(String valor, String padrao) {
        return (valor != null && !valor.isBlank()) ? valor : padrao;
    }

    private String limparCnpj(String cnpj) {
        return cnpj != null ? cnpj.replaceAll("\\D", "") : "";
    }
}
