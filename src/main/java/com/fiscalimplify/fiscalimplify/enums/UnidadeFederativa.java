package com.fiscalimplify.fiscalimplify.enums;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum UnidadeFederativa {
    AC(12, "1200403"), AL(27, "2704302"), AM(13, "1302603"), AP(16, "1600303"),
    BA(29, "2927408"), CE(23, "2304400"), DF(53, "5300108"), ES(32, "3205309"),
    GO(52, "5208707"), MA(21, "2111300"), MG(31, "3106200"), MS(50, "5002704"),
    MT(51, "5103403"), PA(15, "1501402"), PB(25, "2507507"), PE(26, "2611606"),
    PI(22, "2211001"), PR(41, "4113700"), RJ(33, "3304557"), RN(24, "2408102"),
    RO(11, "1100205"), RR(14, "1400100"), RS(43, "4314902"), SC(42, "4205407"),
    SE(28, "2800308"), SP(35, "3550308"), TO(17, "1721000");

    private final int codigoIbge;
    private final String codigoMunicipioCapital;

    public static UnidadeFederativa fromSigla(String sigla) {
        if (sigla == null || sigla.isBlank()) return CE;
        return Arrays.stream(values())
                .filter(uf -> uf.name().equalsIgnoreCase(sigla.trim()))
                .findFirst()
                .orElse(CE);
    }

    public static boolean isValid(String sigla) {
        if (sigla == null || sigla.isBlank()) return false;
        return Arrays.stream(values())
                .anyMatch(uf -> uf.name().equalsIgnoreCase(sigla.trim()));
    }
}