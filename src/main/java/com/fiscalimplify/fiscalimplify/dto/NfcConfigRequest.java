package com.fiscalimplify.fiscalimplify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO responsável pelos dados de configuração de NFC-e na Nuvem Fiscal (CRT, CSC, ambiente).
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Data
public class NfcConfigRequest {

    @NotNull(message = "CRT é obrigatório")
    @Min(1)
    @Max(4)
    private Integer crt = 3;

    @NotNull(message = "ID do CSC é obrigatório")
    @Min(0)
    private Integer idCsc;

    @NotBlank(message = "CSC é obrigatório")
    private String csc;

    @NotBlank(message = "Ambiente é obrigatório")
    @Pattern(regexp = "homologacao|producao", message = "Ambiente deve ser 'homologacao' ou 'producao'")
    private String ambiente = "homologacao";
}
