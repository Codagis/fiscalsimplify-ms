package com.fiscalimplify.fiscalimplify.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO responsável pelos dados de item de nota fiscal (NF-e/NFC-e): descrição, NCM, CFOP, CST, quantidade e valor.
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Data
public class ItemRequest {

    /** Código do produto (SKU ou identificador). Se não informado, usa ITEM+n. */
    @Size(max = 60)
    private String codigo;

    @NotBlank(message = "Descrição do item é obrigatória")
    @Size(max = 120)
    private String descricao;

    @NotBlank(message = "NCM é obrigatório")
    @Size(min = 8, max = 8, message = "NCM deve ter 8 dígitos")
    private String ncm;

    @NotBlank(message = "CFOP é obrigatório")
    @Size(min = 4, max = 4, message = "CFOP deve ter 4 dígitos")
    private String cfop;

    @Size(max = 3)
    private String cst;

    @NotNull(message = "Quantidade é obrigatória")
    @DecimalMin(value = "0.0001", message = "Quantidade deve ser maior que zero")
    private BigDecimal quantidade;

    @NotNull(message = "Valor unitário é obrigatório")
    @DecimalMin(value = "0", message = "Valor unitário não pode ser negativo")
    private BigDecimal valorUnitario;
}
