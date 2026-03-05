package com.fiscalimplify.fiscalimplify.dto;

import com.fiscalimplify.fiscalimplify.validation.ValidUF;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * DTO responsável pelos dados do destinatário de NF-e ou NFC-e (CPF/CNPJ, nome, IE, endereço).
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Data
public class DestinatarioRequest {

    @Size(max = 14)
    private String cnpj;

    @Size(max = 11)
    private String cpf;

    @NotBlank(message = "Nome/razão social do destinatário é obrigatório")
    @Size(max = 60)
    private String nome;

    @Size(max = 14)
    private String ie;

    @Size(max = 60)
    private String logradouro;

    @Size(max = 60)
    private String numero;

    @Size(max = 60)
    private String bairro;

    @Size(max = 60)
    private String municipio;

    @ValidUF
    private String uf;

    @Size(max = 7)
    @Pattern(regexp = "(\\d{7})?", message = "Código do município deve ter 7 dígitos quando informado")
    private String codigoMunicipio;

    @Pattern(regexp = "(\\d{8})?", message = "CEP deve conter 8 dígitos quando informado")
    private String cep;
}
