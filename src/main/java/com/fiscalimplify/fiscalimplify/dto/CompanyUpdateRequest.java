package com.fiscalimplify.fiscalimplify.dto;

import com.fiscalimplify.fiscalimplify.validation.ValidUF;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO responsável pelos dados de atualização parcial de empresa (IE, UF, município, CRT).
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Data
public class CompanyUpdateRequest {

    @Size(max = 14)
    private String inscricaoEstadual;

    @Size(min = 2, max = 2)
    @ValidUF
    private String uf;

    @Pattern(regexp = "\\d{7}", message = "Código do município deve ter 7 dígitos")
    private String codigoMunicipio;

    @Size(max = 100)
    private String nomeMunicipio;

    private Integer crt;
}
