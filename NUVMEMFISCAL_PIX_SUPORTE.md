# NFC-e PIX – Rejeição SEFAZ (dados do cartão)

## Problema
Ao emitir NFC-e com **pagamento PIX** (tPag 17), a SEFAZ rejeita com:
```text
motivo_status -> Rejeicao: Nao informados os dados do cartao de credito/debito nas Formas de Pagamento da Nota Fiscal [Ocorr:1]
```

## Payload enviado pelo Fiscalimplify (correto)
O JSON que enviamos para a API Nuvem Fiscal contém o bloco de pagamento **correto para PIX**:

```json
"pag" : {
  "detPag" : [ {
    "indPag" : 0,
    "tPag" : "17",
    "vPag" : 153.25,
    "xPag" : "PIX"
  } ],
  "vTroco" : 3.25
}
```

- **tPag "17"** = PIX (conforme tabela SEFAZ)
- **Não há** bloco `card` (obrigatório apenas para tPag 03/04)

Ou seja: não informamos cartão porque a forma de pagamento é PIX.

## Conclusão
O **Fiscalimplify envia o JSON correto**. A rejeição indica que o **XML que chega à SEFAZ** está com forma de pagamento **03 ou 04** (cartão) sem o grupo de dados do cartão.

Possíveis causas no lado Nuvem Fiscal:
1. Conversão JSON → XML alterando ou ignorando o `tPag` enviado (ex.: default 03/04).
2. Uso de template ou merge que inclui um `detPag` com tPag 03/04.
3. Bug na montagem do grupo `pag` no XML da NFC-e.

## O que pedir ao suporte Nuvem Fiscal
1. **Confirmar o XML** da NFC-e que foi enviado à SEFAZ (principalmente o grupo `<pag>` / `<detPag>`).
2. **Esclarecer** se há regra ou default que altere `tPag` 17 para 03/04.
3. **Garantir** que, quando o cliente envia `tPag` "17" (e sem `card`), o XML gerado tenha `<tPag>17</tPag>` e **não** 03/04.

Envie este arquivo e o exemplo de JSON do bloco `pag` acima ao abrir o chamado.
