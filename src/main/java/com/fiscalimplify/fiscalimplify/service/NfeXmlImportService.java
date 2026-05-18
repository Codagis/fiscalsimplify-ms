package com.fiscalimplify.fiscalimplify.service;

import com.fiscalimplify.fiscalimplify.dto.DestinatarioRequest;
import com.fiscalimplify.fiscalimplify.dto.ItemRequest;
import com.fiscalimplify.fiscalimplify.dto.NfeImportXmlResponse;
import com.fiscalimplify.fiscalimplify.dto.NfeRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.XMLConstants;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class NfeXmlImportService {

    private final FiscalService fiscalService;

    public NfeImportXmlResponse emitirNfeFromXml(MultipartFile xmlFile) {
        if (xmlFile == null || xmlFile.isEmpty()) {
            throw new IllegalArgumentException("XML é obrigatório.");
        }

        NfeRequest req = parse(xmlFile);
        return NfeImportXmlResponse.builder()
                .parsedRequest(req)
                .emissionResult(fiscalService.emitirNfe(req))
                .build();
    }

    private NfeRequest parse(MultipartFile xmlFile) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            factory.setNamespaceAware(true);
            try {
                factory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true);
            } catch (Exception ignored) {
                // Ignora se o parser não suportar a feature; ainda seguimos com namespaceAware.
            }
            Document doc = factory.newDocumentBuilder().parse(xmlFile.getInputStream());
            doc.getDocumentElement().normalize();

            Element infNFe = firstElement(doc, "infNFe");
            Element ide = firstChild(infNFe, "ide");
            Element emit = firstChild(infNFe, "emit");
            Element dest = firstChild(infNFe, "dest");

            String cnpjEmit = text(firstChild(emit, "CNPJ"));
            String ieEmit = textOrNull(childOrNull(emit, "IE"));
            String serie = text(firstChild(ide, "serie"));
            String natOp = text(firstChild(ide, "natOp"));

            DestinatarioRequest destinatario = new DestinatarioRequest();
            destinatario.setNome(text(firstChild(dest, "xNome")));
            String cnpjDest = textOrNull(childOrNull(dest, "CNPJ"));
            String cpfDest = textOrNull(childOrNull(dest, "CPF"));
            if (cnpjDest != null) destinatario.setCnpj(cleanDigits(cnpjDest));
            if (cpfDest != null) destinatario.setCpf(cleanDigits(cpfDest));
            destinatario.setIe(textOrNull(childOrNull(dest, "IE")));
            Element indIEDestEl = childOrNull(dest, "indIEDest");
            if (indIEDestEl != null) {
                String indRaw = textOrNull(indIEDestEl);
                if (indRaw != null && indRaw.matches("\\d")) {
                    destinatario.setIndIEDest(Integer.parseInt(indRaw));
                }
            }

            if ((destinatario.getCnpj() == null || destinatario.getCnpj().isBlank())
                    && (destinatario.getCpf() == null || destinatario.getCpf().isBlank())) {
                throw new IllegalArgumentException("XML inválido: tag ausente CNPJ/CPF do destinatário");
            }

            Element enderDest = childOrNull(dest, "enderDest");
            if (enderDest != null) {
                destinatario.setLogradouro(textOrNull(childOrNull(enderDest, "xLgr")));
                destinatario.setNumero(textOrNull(childOrNull(enderDest, "nro")));
                destinatario.setBairro(textOrNull(childOrNull(enderDest, "xBairro")));
                destinatario.setMunicipio(textOrNull(childOrNull(enderDest, "xMun")));
                destinatario.setUf(textOrNull(childOrNull(enderDest, "UF")));
                destinatario.setCep(cleanDigits(textOrNull(childOrNull(enderDest, "CEP"))));
                destinatario.setCodigoMunicipio(cleanDigits(textOrNull(childOrNull(enderDest, "cMun"))));
            }

            List<ItemRequest> itens = new ArrayList<>();
            NodeList detList = infNFe.getElementsByTagName("det");
            for (int i = 0; i < detList.getLength(); i++) {
                Node detNode = detList.item(i);
                if (!(detNode instanceof Element detEl)) continue;
                Element prod = childOrNull(detEl, "prod");
                if (prod == null) continue;
                ItemRequest item = new ItemRequest();
                item.setCodigo(textOrNull(childOrNull(prod, "cProd")));
                item.setDescricao(text(firstChild(prod, "xProd")));
                item.setNcm(cleanDigits(text(firstChild(prod, "NCM"))));
                String cfop = textOrNull(childOrNull(prod, "CFOP"));
                if (cfop != null) item.setCfop(cleanDigits(cfop));
                String qCom = textOrNull(childOrNull(prod, "qCom"));
                String vUnCom = textOrNull(childOrNull(prod, "vUnCom"));
                item.setQuantidade(qCom != null ? new BigDecimal(qCom.replace(",", ".")) : BigDecimal.ONE);
                item.setValorUnitario(vUnCom != null ? new BigDecimal(vUnCom.replace(",", ".")) : BigDecimal.ZERO);
                itens.add(item);
            }

            NfeRequest req = new NfeRequest();
            req.setCnpjEmitente(cleanDigits(cnpjEmit));
            req.setIeEmitente(ieEmit != null ? ieEmit.trim() : null);
            req.setSerie(Integer.parseInt(cleanDigits(serie)));
            req.setNaturezaOperacao(natOp != null && !natOp.isBlank() ? natOp.trim() : "VENDA");
            req.setDestinatario(destinatario);
            req.setItens(itens);

            return req;
        } catch (Exception e) {
            throw new IllegalArgumentException("Falha ao ler XML da NF-e: " + e.getMessage(), e);
        }
    }

    private static String cleanDigits(String s) {
        if (s == null) return null;
        String d = s.replaceAll("\\D", "");
        return d.isBlank() ? null : d;
    }

    private static Element firstElement(Document doc, String tag) {
        NodeList list = doc.getElementsByTagNameNS("*", tag);
        if (list.getLength() == 0) throw new IllegalArgumentException("XML inválido: tag ausente " + tag);
        return (Element) list.item(0);
    }

    private static Element firstChild(Element parent, String tag) {
        if (parent == null) throw new IllegalArgumentException("XML inválido: nó pai nulo ao buscar " + tag);
        NodeList list = parent.getElementsByTagNameNS("*", tag);
        if (list.getLength() == 0) throw new IllegalArgumentException("XML inválido: tag ausente " + tag);
        return (Element) list.item(0);
    }

    private static Element childOrNull(Element parent, String tag) {
        if (parent == null) return null;
        NodeList list = parent.getElementsByTagNameNS("*", tag);
        if (list.getLength() == 0) return null;
        return (Element) list.item(0);
    }

    private static String text(Element el) {
        String v = textOrNull(el);
        if (v == null || v.isBlank()) throw new IllegalArgumentException("XML inválido: valor obrigatório ausente");
        return v.trim();
    }

    private static String textOrNull(Element el) {
        if (el == null) return null;
        String v = el.getTextContent();
        return v != null ? v.trim() : null;
    }
}

