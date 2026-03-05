package com.fiscalimplify.fiscalimplify.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

/**
 * DTO responsável pelos dados de forma de pagamento da NFC-e (código tPag e valor).
 *
 * @author Fiscalimplify
 * @version 1.0
 */
@Data
public class PagamentoRequest {

    @NotBlank(message = "Forma de pagamento é obrigatória")
    @Size(max = 2)
    private String forma;

    @NotNull(message = "Valor é obrigatório")
    @DecimalMin(value = "0", message = "Valor não pode ser negativo")
    private BigDecimal valor;

    /** CNPJ da adquirente (14 dígitos). Obrigatório quando forma é 03 ou 04 e se exige dados do cartão. */
    @Size(max = 14)
    private String cnpjAdquirente;

    /** Bandeira: 01=Visa, 02=Master, 03=Amex, 04=Sorocred, 99=Outros */
    @Size(max = 2)
    private String tBand;

    /** Código de autorização da transação (1-20 caracteres) */
    @Size(max = 20)
    private String cAut;

    /** Tipo de integração: 1=integrado, 2=não integrado (POS) */
    private Integer tpIntegracao;
}
