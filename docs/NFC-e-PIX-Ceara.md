# NFC-e (modelo 65) – PIX no Ceará

## 1. PIX exige dados da adquirente no Ceará?

**Não.** Para pagamento via **PIX** (tPag = "17") **não** é obrigatório enviar:

- CNPJ da adquirente  
- Código de autorização  
- Número da transação / NSU  
- Bandeira  
- Grupo `<card>`

Essas informações são obrigatórias **apenas** para:

- **"03"** → Cartão de Crédito  
- **"04"** → Cartão de Débito  

Conforme o **layout da NFC-e** e o **Manual de Orientação do Contribuinte (MOC)**.

---

## 2. Por que às vezes a SEFAZ pede “dados do cartão”?

Em alguns fluxos, a SEFAZ-CE pode tratar pagamento eletrônico como “integrado” e exigir grupo `<card>` quando:

- a operação é **presencial** (indPres = 1) e  
- o sistema interpreta o pagamento como “eletrônico integrado” (ex.: cartão).

**Regra oficial:**

- **PIX não é cartão.**  
- **PIX não exige adquirente** nem grupo `<card>`.

Por isso, para PIX o sistema envia **apenas** `tPag` e `vPag`, **sem** grupo `<card>`, `indPag` ou qualquer campo de adquirente, para que a SEFAZ não interprete como integração com cartão.

---

## 3. Base normativa – Ceará

- **IN SEFAZ-CE nº 87/2025** (vinculação do pagamento ao documento fiscal eletrônico):  
  **PIX estático** (e formas que não geram código de autorização único por transação) estão **dispensados** da vinculação automática. Ou seja, não há obrigação de enviar dados de adquirente/cartão para PIX nesses casos.

- **MOC NFC-e**: grupo `<card>` e dados de adquirente são exigidos somente para tPag 03 e 04 (cartão de crédito/débito).

---

## 4. O que este projeto envia para PIX

Para **um único pagamento PIX** (tPag = "17"):

- **Enviado:** `tPag` = "17", `vPag` = valor da nota (vNF), sem troco.
- **Não enviado:** `indPag`, grupo `card`, CNPJ adquirente, autorização, bandeira, NSU.

Exemplo do bloco `pag`:

```json
"pag": {
  "detPag": [
    {
      "tPag": "17",
      "vPag": 150.00
    }
  ]
}
```

Sem `vTroco` quando o pagamento é igual ao valor da nota (vNF).

---

## 5. Em caso de rejeição

Se a SEFAZ ainda rejeitar pedindo “dados do cartão” para PIX:

1. Confirmar que o **XML** enviado à SEFAZ contém **`<tPag>17</tPag>`** (e não 03/04).  
2. Confirmar que **não** existe grupo `<card>` no `detPag` desse pagamento.  
3. Encaminhar à SEFAZ-CE a **IN 87/2025** e o **MOC da NFC-e**, informando que PIX (17) está dispensado da vinculação e que o grupo `<card>` é exigido apenas para 03/04.

Implementação de referência: `FiscalService.mapearPagamento()` (PIX = apenas tPag + vPag).
