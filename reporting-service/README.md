# reporting-service

Microservice Quarkus responsavel pelo painel financeiro e relatorios consolidados por tenant.

O servico materializa um read model proprio para consultas rapidas de dashboard, tabela de lancamentos e graficos. A ingestao inicial entra por endpoint interno protegido por `X-Internal-Token`; o caminho natural de evolucao e o `core-service` publicar/encaminhar os pedidos normalizados dos conectores para este read model.

## Endpoints locais

- API: `http://localhost:8087/reports`
- Health: `http://localhost:8087/q/health`
- Metrics: `http://localhost:8087/q/metrics`
- OpenAPI: `http://localhost:8087/q/openapi`
- Swagger UI: `http://localhost:8087/q/swagger-ui`

## Contratos

- `GET /reports`: status do servico.
- `GET /reports/tenants/{tenantId}/dashboard`: painel consolidado com cards, tabela inicial, graficos e filtros.
- `GET /reports/tenants/{tenantId}/summary`: cards financeiros.
- `GET /reports/tenants/{tenantId}/entries`: tabela de lancamentos com busca, ordenacao, filtros e paginacao.
- `GET /reports/tenants/{tenantId}/charts/monthly-evolution`: grafico de evolucao mensal.
- `GET /reports/tenants/{tenantId}/charts/platform-comparison`: comparativo entre marketplaces/plataformas.
- `GET /reports/tenants/{tenantId}/filters`: plataformas, formas de pagamento e status disponiveis.
- `POST /reports/tenants/{tenantId}/manual-import/entries`: importacao manual de lancamento normalizado para plataformas sem API.
- `POST /reports/tenants/{tenantId}/integrations/entries`: API publica para vendedor/contador integrar sistemas proprios.
- `GET /reports/tenants/{tenantId}/exports/monthly?month=YYYY-MM&format=pdf|xlsx|csv`: exportacao mensal consolidada com todos os marketplaces.
- `GET /reports/tenants/{tenantId}/exports/platforms/{platform}?from=YYYY-MM-DD&to=YYYY-MM-DD&format=pdf|xlsx|csv`: exportacao filtrada por marketplace/modulo.
- `POST /reports/internal/entries`: ingestao interna de lancamentos materializados.
- `GET /reports/internal/tenants/{tenantId}/summary`: resumo interno usado por automacoes.
- `GET /reports/internal/tenants/{tenantId}/payment-releases`: pagamentos a liberar usados pelos alertas automaticos.

Filtros suportados nos endpoints de leitura:

- `from` e `to`: periodo por `sale_date`.
- `platform`: marketplace/plataforma.
- `paymentMethod`: `PIX`, `CREDIT_CARD`, `DEBIT_CARD`, `BOLETO`, `BANK_TRANSFER`, `MARKETPLACE_BALANCE` ou `OTHER`.
- `status`: `PAID`, `PENDING_RELEASE`, `RECEIVED`, `CANCELLED` ou `REFUNDED`.
- `search`: busca em `order_id`, `buyer_name` e `invoice_number`.
- `sort`, `direction`, `page`, `size`: ordenacao e paginacao da tabela.

## Controle de acesso

Endpoints `/reports/tenants/{tenantId}/...` exigem Bearer JWT emitido pelo `auth-service`. O `tenantId` do path precisa bater com o claim `tenant_id`.

- `ADMIN`, `VENDEDOR` e `CONTADOR`: podem consultar relatorios do proprio tenant.
- `ADMIN`, `VENDEDOR` e `CONTADOR`: podem enviar lancamentos pelos endpoints de importacao/integracao publica. O tenant continua vindo do JWT/path, nunca do body.
- `/reports/internal/**`: aceita apenas `X-Internal-Token` e fica bloqueado no gateway publico.

## Cards financeiros

- `gross_value`: faturado bruto.
- `received_value`: recebido/liberado.
- `fee_value`: taxas.
- `receivable_value`: a receber.
- `entry_count`: quantidade de lancamentos considerados.

## Exportacao

O motor unico de exportacao reutiliza o mesmo read model dos relatorios e gera:

- PDF com capa, resumo financeiro, consolidado por marketplace e tabela em layout voltado para contador.
- Excel `.xlsx` com aba `Resumo` e uma aba por marketplace.
- CSV em UTF-8 com colunas estaveis para integracoes externas.

Quando `format` nao e informado, o padrao e `pdf`. O parametro `month` usa o formato `YYYY-MM`.

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

## Build

```shell
./mvnw verify
```
