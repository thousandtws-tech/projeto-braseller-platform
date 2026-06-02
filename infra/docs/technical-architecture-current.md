# Documento Tecnico de Arquitetura Atual

Sistema Modular Multi-Plataforma e Motor Contabil (DRE)

Versao convertida para o projeto atual: Quarkus, Keycloak, PostgreSQL/Neon e Cloudinary.

## Decisao de Stack

O documento original foi adaptado para a arquitetura real deste repositorio. Nao usamos Supabase Auth nem AWS S3 neste projeto.

| Camada | Projeto atual | Observacao |
| --- | --- | --- |
| Backend/API | Microservices Quarkus Java 21 | `gateway-api`, `auth-service`, `user-service`, `core-service`, `reporting-service`, `billing-service`, `notification-service`. |
| Auth | Keycloak + JWT interno | Keycloak cuida de credenciais/OAuth; `auth-service` emite o JWT tenant-aware da plataforma. |
| Banco | PostgreSQL por servico, local ou Neon | Database-per-service com Flyway. Sem Supabase. |
| Storage de comprovantes | Cloudinary | Upload direto assinado pelo `reporting-service`; despesas persistem `public_id` e `secure_url`. |
| Assinatura digital | Clicksign API 3.0 Envelope | Coleta assinatura digital do contador no balanco/DRE mensal. |
| Processamento interno | Quarkus Scheduler + REST interno + tabelas PostgreSQL | Sync de conectores, materializacao de lancamentos no Reporting, calculo de DRE e eventos de nova venda sem broker no MVP. |
| Observabilidade | Prometheus + Grafana | Health, metrics e dashboards locais. |

## Fase 1 Convertida

Escopo MVP mantido no projeto atual:

- Core: autenticacao, contexto multi-tenant, banco por servico e gateway.
- Fiscal/DRE: regime tributario, despesas com comprovante Cloudinary obrigatorio, DRE simplificada e fechamento mensal.
- Mercado Livre: conector por adapter em `core-service`, sem `if platform == ml` no motor contabil.
- Contador: acesso `CONTADOR` read-only e assinatura de fechamento mensal. O provedor escolhido para assinatura digital real e a Clicksign.

Nao foi criado microservice novo para fiscal/contabil na Fase 1. O nucleo fiscal fica no `reporting-service` porque a DRE depende do read model financeiro ja materializado. Um `accounting-service` separado fica reservado para NF-e/SPED, conciliacao bancaria real, apuracao fiscal propria ou workflow contabil independente.

## Cloudinary no Lugar de S3

Fluxo recomendado para comprovantes de despesa:

1. Frontend chama `GET /api/reports/tenants/{tenantId}/expenses/upload-signature`.
2. `reporting-service` valida JWT e papel de escrita.
3. `reporting-service` retorna assinatura, `cloud_name`, `api_key`, `folder` e `upload_url`.
4. Frontend envia o arquivo direto ao Cloudinary.
5. Frontend cria a despesa em `POST /api/reports/tenants/{tenantId}/expenses` com `public_id`, `secure_url`, `resource_type`, `original_filename`, `content_type` e `size_bytes`.
6. Backend rejeita qualquer despesa sem `public_id` e `secure_url`.

Variaveis necessarias:

```properties
CLOUDINARY_CLOUD_NAME=...
CLOUDINARY_API_KEY=...
CLOUDINARY_API_SECRET=...
CLOUDINARY_EXPENSE_FOLDER=brasaller/despesas
CLOUDINARY_RESOURCE_TYPE=auto
```

## Regras Criticas

- Despesa manual sem comprovante Cloudinary nao entra na DRE.
- Fechamento assinado pelo contador, via Clicksign na versao completa, bloqueia edicoes em pedidos, taxas e despesas daquele mes.
- Cancelamentos retroativos devem ser lancados no mes corrente.
- Campos financeiros persistidos usam `DECIMAL(10,2)`, nunca `FLOAT`, `REAL` ou `DOUBLE`.
- Tokens de marketplace ficam criptografados com AES-256 no `core-service`.
- O Core depende da interface `MarketplaceConnector`; conectores especificos ficam em adapters.

## Banco de Dados - Secao 08 Convertida

O documento original descreve tabelas logicas. No projeto atual elas foram convertidas para database-per-service, sem Supabase e sem S3.

| Tabela do documento | Implementacao atual | Status |
| --- | --- | --- |
| `tenants` | `user-service.tenants` | Feito. Conta da empresa e isolamento por tenant. |
| `tax_profiles` | `reporting-service.tenant_fiscal_profiles` | Feito para MVP: regime tributario e aliquota estimada. CNAE fica para evolucao fiscal. |
| `orders` | `reporting-service.report_entries` | Feito como read model financeiro normalizado por `tenant_id`, `platform` e `order_id`. |
| `inventory_items` | Ainda nao implementado | Futuro. Pertence a fase CMV/estoque com XML de NF de entrada. |
| `expenses` | `reporting-service.expense_entries` | Feito. Despesas operacionais, bancarias e impostos manuais. |
| `expense_attachments` | Colunas `attachment_*` em `expense_entries` | Feito para MVP. Como cada despesa exige um comprovante, o anexo Cloudinary ficou embutido no registro. |
| `dre_reports` | `dre_calculation_jobs` + DRE sob demanda + `accounting_period_closings` | Feito para MVP: DRE sob demanda continua disponivel, calculos assincronos ficam materializados em jobs, e o fechamento assinado trava o mes. Relatorio final assinado/versionado fica para etapa Clicksign. |

Tabelas principais ja existentes:

| Tabela | Servico | Papel |
| --- | --- | --- |
| `tenants`, `user_accounts`, `user_roles`, `accountant_access` | `user-service` | Empresas, usuarios e acesso do contador. |
| `auth_identities`, `auth_sessions` | `auth-service` | Identidades sincronizadas com Keycloak e sessoes. |
| `marketplace_connector_tokens` | `core-service` | Tokens criptografados por tenant/conector. |
| `connector_sync_jobs` | `core-service` | Status e resultado de sincronizacoes de conectores processadas em background pelo scheduler. |
| `report_entries` | `reporting-service` | Pedidos/lancamentos normalizados para relatorios e DRE. |
| `tenant_fiscal_profiles` | `reporting-service` | Regime tributario e aliquota estimada. |
| `expense_entries` | `reporting-service` | Despesas com metadados Cloudinary obrigatorios. |
| `dre_calculation_jobs` | `reporting-service` | Jobs e resultados materializados de DRE calculada em background pelo scheduler. |
| `accounting_period_closings` | `reporting-service` | Assinatura e lock mensal do contador. |

## Roadmap Ajustado

- Fase 1: Mercado Livre + Core contabil MVP, usando Cloudinary e Keycloak.
- Fase 2: Shopee via novo adapter do `core-service`, sem alterar o motor contabil.
- Fase 3: CMV/estoque, XML de NF e assinatura digital Clicksign integrada.
- Fase 4: Amazon SP-API, Open Finance/OFX e conciliacao bancaria.

## Assinatura Digital - Clicksign

A plataforma escolhida para coleta da assinatura digital do contador no balanco/DRE mensal e a Clicksign.

Fluxo alvo:

1. `reporting-service` gera o balanco/DRE mensal em PDF.
2. `reporting-service` cria um Envelope na Clicksign.
3. O PDF do balanco/DRE e anexado ao Envelope como documento.
4. O contador e adicionado como signatario.
5. O sistema define os requisitos de qualificacao e autenticacao do signatario.
6. A Clicksign notifica o contador para assinatura.
7. O webhook da Clicksign informa assinatura/conclusao do documento.
8. O backend registra o fechamento em `accounting_period_closings`, guarda os identificadores Clicksign e trava o periodo.
9. O PDF assinado passa a ser o documento contabil final do mes.

Endpoint de recebimento dos eventos:

```text
POST /api/reports/webhooks/clicksign
```

Na tela de webhooks da Clicksign, cadastre a URL publica do gateway, por exemplo `https://SEU-DOMINIO/api/reports/webhooks/clicksign`. Depois copie o HMAC SHA256 Secret gerado pela Clicksign para `CLICKSIGN_WEBHOOK_SECRET`.

Variaveis reservadas:

```properties
CLICKSIGN_BASE_URL=https://sandbox.clicksign.com/api/v3
CLICKSIGN_ACCESS_TOKEN=...
CLICKSIGN_WEBHOOK_SECRET=...
CLICKSIGN_DEFAULT_DEADLINE_DAYS=7
```

Observacao de implementacao: o endpoint atual `POST /reports/tenants/{tenantId}/closings/{month}/sign` registra o fechamento manual/hash e trava o periodo. A evolucao Clicksign deve trocar esse fechamento direto por um fluxo assincrono: solicitar assinatura, aguardar webhook de conclusao e entao executar o lock.

## Pendencias e Evolucao

Esta lista separa o que ainda falta apos a conversao do documento original para o projeto atual.

### Banco / Secao 08

| Item | Status | Observacao |
| --- | --- | --- |
| `inventory_items` | Pendente | Estoque, SKU, custo medio e CMV via XML ainda nao existem. |
| CNAE no perfil fiscal | Pendente | `tenant_fiscal_profiles` cobre regime e aliquota estimada; CNAE/faixas detalhadas ficam para evolucao fiscal. |
| `dre_reports` materializado | Parcialmente feito | Jobs de DRE ficam materializados em `dre_calculation_jobs`; falta o relatorio final assinado/versionado via Clicksign. |
| RLS nativo PostgreSQL | Pendente | O isolamento atual e feito por JWT + validacao tenant-aware na aplicacao. |
| `expense_attachments` separado | Opcional | No MVP cada despesa exige um comprovante, entao os campos Cloudinary ficam em `expense_entries`. Separar tabela so e necessario para multiplos anexos por despesa. |

### Funcional

| Item | Status | Observacao |
| --- | --- | --- |
| Parser XML de NF de entrada | Pendente | Necessario para estoque e CMV real. |
| Integracao Receita Federal/CNPJ | Pendente | Automatiza dados fiscais da empresa. |
| Assinatura digital Clicksign | Provider definido, implementacao pendente | Criar envelope, anexar balanco/DRE PDF, cadastrar contador como signatario, receber webhook de conclusao, salvar PDF assinado e travar periodo. |
| Upload real Cloudinary no frontend | Pendente externo ao backend | Backend ja gera assinatura de upload e valida metadados. |
| Credenciais Cloudinary reais | Pendente operacional | Preencher `CLOUDINARY_CLOUD_NAME`, `CLOUDINARY_API_KEY` e `CLOUDINARY_API_SECRET`. |

### Marketplaces e Integracoes Futuras

| Item | Status | Observacao |
| --- | --- | --- |
| Shopee | Pendente | Novo adapter em `core-service`, com OAuth/HMAC. |
| Amazon SP-API | Pendente | Novo adapter em `core-service`, com OAuth e AWS Signature V4. |
| Open Finance/OFX | Pendente | Captura tarifas bancarias e conciliacao. |
| Conciliacao bancaria | Pendente | Comparar extrato, repasses e lancamentos. |
| Fiscal completo | Pendente | NF-e/SPED/impostos reais e workflow contabil dedicado. |

Resumo: o MVP da Fase 1 esta encaminhado com multi-tenant, Keycloak, Mercado Livre, despesas Cloudinary, DRE, acesso do contador e lock mensal. O backlog restante e principalmente CMV/estoque, fiscal avancado, integracao Clicksign, RLS nativo e novas integracoes.
