package com.fiscalimplify.fiscalimplify.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

/**
 * DTO responsável pelos dados de emissão de NF-e (emitente, destinatário obrigatório e itens).
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Data
public class NfeRequest {

    @NotBlank(message = "CNPJ do emitente é obrigatório")
    @Pattern(regexp = "\\d{14}", message = "CNPJ deve conter 14 dígitos")
    private String cnpjEmitente;

    @Size(max = 14)
    private String ieEmitente;

    @NotNull(message = "Série é obrigatória")
    private Integer serie;

    @NotBlank(message = "Natureza da operação é obrigatória")
    @Size(max = 100)
    private String naturezaOperacao;

    @NotNull(message = "Destinatário é obrigatório")
    @Valid
    private DestinatarioRequest destinatario;

    @NotEmpty(message = "Lista de itens não pode ser vazia")
    @Valid
    private List<ItemRequest> itens;
}
