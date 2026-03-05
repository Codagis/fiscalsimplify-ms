package com.fiscalimplify.fiscalimplify.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * DTO responsável pelos dados de cadastro de certificado digital A1 (.pfx/.p12) na Nuvem Fiscal.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Data
public class CertificadoRequest {

    @NotBlank(message = "Certificado em Base64 é obrigatório")
    private String certificado;

    @NotBlank(message = "Senha do certificado é obrigatória")
    private String password;
}
