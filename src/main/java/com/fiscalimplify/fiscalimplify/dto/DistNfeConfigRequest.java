package com.fiscalimplify.fiscalimplify.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * DTO de configuração de Distribuição NF-e (DF-e) na Nuvem Fiscal.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Data
public class DistNfeConfigRequest {

    @NotBlank(message = "Ambiente é obrigatório")
    @Pattern(regexp = "homologacao|producao", message = "Ambiente deve ser 'homologacao' ou 'producao'")
    private String ambiente = "homologacao";

    private Boolean distribuicaoAutomatica = false;

    @Min(1)
    @Max(24)
    private Integer distribuicaoIntervaloHoras = 24;

    private Boolean cienciaAutomatica = false;
}
