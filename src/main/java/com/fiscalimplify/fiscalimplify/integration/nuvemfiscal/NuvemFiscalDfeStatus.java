package com.fiscalimplify.fiscalimplify.integration.nuvemfiscal;

import java.util.Map;

/**
 * Interpreta status da Nuvem Fiscal (NFC-e / NF-e) apos emissao.
 */
public final class NuvemFiscalDfeStatus {

    private NuvemFiscalDfeStatus() {
    }

    public static String extrairStatus(Map<String, Object> doc) {
        if (doc == null || doc.isEmpty()) {
            return "";
        }
        Object status = doc.get("status");
        if (status != null && !status.toString().isBlank()) {
            return status.toString().trim();
        }
        Object situacao = doc.get("situacao");
        return situacao != null ? situacao.toString().trim() : "";
    }

    public static String extrairMotivo(Map<String, Object> doc) {
        if (doc == null) {
            return "";
        }
        Object motivo = doc.get("motivo_status");
        if (motivo != null && !motivo.toString().isBlank()) {
            return motivo.toString().trim();
        }
        Object auth = doc.get("autorizacao");
        if (auth instanceof Map<?, ?> authMap) {
            Object m = authMap.get("motivo_status");
            if (m != null && !m.toString().isBlank()) {
                return m.toString().trim();
            }
            Object codigo = authMap.get("codigo_status");
            if (codigo != null) {
                return codigo.toString().trim();
            }
        }
        return "";
    }

    public static boolean isAutorizado(Map<String, Object> doc) {
        String status = extrairStatus(doc).toLowerCase();
        String motivo = extrairMotivo(doc).toLowerCase();
        if (isRejeitado(status, motivo)) {
            return false;
        }
        if (status.contains("autoriz") || status.contains("registrad") || status.contains("aprovad")) {
            return true;
        }
        Object auth = doc != null ? doc.get("autorizacao") : null;
        if (auth instanceof Map<?, ?> authMap) {
            Object authStatus = authMap.get("status");
            if (authStatus != null) {
                String as = authStatus.toString().toLowerCase();
                if (as.contains("autoriz") || as.contains("registrad")) {
                    return true;
                }
            }
        }
        Object chave = doc != null ? doc.get("chave") : null;
        return chave != null && !chave.toString().isBlank();
    }

    public static boolean isRejeitado(Map<String, Object> doc) {
        return isRejeitado(extrairStatus(doc), extrairMotivo(doc));
    }

    public static boolean isRejeitado(String status, String motivo) {
        String s = status != null ? status.toLowerCase() : "";
        String m = motivo != null ? motivo.toLowerCase() : "";
        return s.contains("rejeit") || s.contains("denegad")
                || m.contains("rejeic") || m.contains("rejeit")
                || "erro".equals(s);
    }

    public static boolean isPendente(Map<String, Object> doc) {
        if (isAutorizado(doc) || isRejeitado(doc)) {
            return false;
        }
        String status = extrairStatus(doc).toLowerCase();
        return status.isBlank()
                || status.contains("pendent")
                || status.contains("process")
                || status.contains("enviad")
                || status.contains("aguard");
    }
}
