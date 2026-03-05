package com.fiscalimplify.fiscalimplify.dto;

import com.fiscalimplify.fiscalimplify.validation.ValidUF;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO responsável pelos dados de cadastro de empresa (CNPJ, razão social, IE, UF, município, CRT).
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Data
public class CompanyRequest {

    @NotBlank(message = "CNPJ é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 dígitos numéricos")
    private String cnpj;

    @NotBlank(message = "Razão social é obrigatória")
    @Size(max = 200)
    private String razaoSocial;

    @Size(max = 200)
    private String nomeFantasia;

    @Size(max = 14)
    private String inscricaoEstadual;

    @Size(min = 2, max = 2)
    @ValidUF
    private String uf;

    @Size(min = 7, max = 7)
    @Pattern(regexp = "\\d{7}", message = "Código do município deve ter 7 dígitos (ex: 2304400)")
    private String codigoMunicipio;

    @Size(max = 100)
    private String nomeMunicipio;

    private Integer crt;
}
