# Postman - Fiscalimplify API

## Importar no Postman

1. **File → Import** (ou arraste os arquivos)
2. Selecione:
   - `Fiscalimplify.postman_collection.json`
   - `Fiscalimplify-Local.postman_environment.json`
3. Selecione o ambiente **Fiscalimplify - Local** no canto superior direito

## Ordem de execução

### 1. Cadastrar empresa (obrigatório)

Execute **01 - Cadastrar Empresa** dentro da pasta **Empresas**.

- O CNPJ será salvo automaticamente na variável `cnpjEmitente`
- A empresa é registrada na API e na Nuvem Fiscal (sandbox)

### 2. Cadastrar certificado digital (obrigatório)

Execute **04 - Cadastrar Certificado Digital** dentro da pasta **Empresas**.

- Certificado A1 (.pfx ou .p12) é necessário para emissão de NFe/NFC-e
- Converta o arquivo para Base64 e substitua no campo `certificado`
- Informe a senha do certificado em `password`

**Converter .pfx para Base64:**
- **PowerShell:** `[Convert]::ToBase64String([IO.File]::ReadAllBytes('c:\caminho\arquivo.pfx'))`
- **Linux/Mac:** `base64 -i arquivo.pfx -o certificado.txt`

### 3. Emitir notas

- **NF-e:** Execute **01 - Emitir NF-e (Completa)** ou **02 - Emitir NF-e (Destinatário CPF)**
- **NFC-e:** Execute **01 - Emitir NFC-e (Completa)**, **02 - Emitir NFC-e (Múltiplos Pagamentos)** ou **03 - Emitir NFC-e (Com Destinatário)**

### 4. Obter e imprimir PDF da nota fiscal

- **NF-e:** Execute **01b - Obter PDF da NF-e (DANFE)** — o id é salvo em `{{nfeId}}` após emitir
- **NFC-e:** Execute **01b - Obter PDF da NFC-e** — o id é salvo em `{{nfceId}}` após emitir

O PDF retorna no padrão DANFE/DANFC-e (estrutura SEFAZ). Use **Send and Download** no Postman para salvar o arquivo ou abra a URL no navegador para visualizar e imprimir (Ctrl+P).

- `GET /nfe/{id}/pdf` — abre no navegador para impressão
- `GET /nfe/{id}/pdf?download=true` — força download do arquivo

## Formato do JSON

A API Fiscalimplify aceita um JSON **simplificado**. O backend converte internamente para a estrutura SEFAZ (infNFe com det, pag.detPag) exigida pela Nuvem Fiscal. Você **não** precisa enviar infNFe, det ou pag.detPag diretamente.

## Dados de teste (homologação SEFAZ)

Os exemplos seguem o padrão exigido pela SEFAZ em ambiente de homologação:

- **Nome destinatário NF-e:** `EMITIDA EM AMBIENTE DE HOMOLOGACAO - SEM VALOR FISCAL`
- **CNPJ destinatário:** `00000000000191` (teste)
- **CPF destinatário:** `12345678909` (teste)
- **NCM/CFOP/CST:** códigos válidos para mercadorias de teste

## Formas de pagamento (NFC-e)

| Código | Descrição   |
|--------|-------------|
| 01     | Dinheiro    |
| 02     | Cheque      |
| 03     | Cartão crédito |
| 04     | Cartão débito  |
| 99     | Outros      |

## Pré-requisitos

- API rodando em `http://localhost:8080`
- Variáveis `NUVEM_CLIENT_ID` e `NUVEM_CLIENT_SECRET` configuradas
- Banco PostgreSQL ativo
