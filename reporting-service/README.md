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
- `GET /reports/tenants/{tenantId}/fiscal-profile`: consulta regime tributario e aliquota estimada.
- `PUT /reports/tenants/{tenantId}/fiscal-profile`: cadastra ou atualiza o perfil fiscal.
- `GET /reports/tenants/{tenantId}/expenses`: lista despesas por periodo/categoria.
- `GET /reports/tenants/{tenantId}/expenses/upload-signature`: gera assinatura para upload direto do comprovante no Cloudinary.
- `POST /reports/tenants/{tenantId}/expenses`: lanca despesa com anexo Cloudinary obrigatorio.
- `GET /reports/tenants/{tenantId}/expenses/{expenseId}`: detalha despesa.
- `PUT /reports/tenants/{tenantId}/expenses/{expenseId}`: atualiza despesa.
- `DELETE /reports/tenants/{tenantId}/expenses/{expenseId}`: remove despesa.
- `GET /reports/tenants/{tenantId}/dre`: gera DRE simplificada por periodo.
- `GET /reports/tenants/{tenantId}/closings/{month}`: consulta fechamento assinado no formato `YYYY-MM`.
- `POST /reports/tenants/{tenantId}/closings/{month}/sign`: contador assina o fechamento e trava o mes.
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
- `ADMIN` e `VENDEDOR`: podem alterar perfil fiscal e despesas.
- `CONTADOR`: pode consultar DRE, perfil fiscal e despesas do proprio tenant, mas nao altera esses cadastros.
- `CONTADOR`: pode assinar o fechamento mensal, tornando imutaveis os lancamentos e despesas do periodo.
- `ADMIN`, `VENDEDOR` e `CONTADOR`: podem enviar lancamentos pelos endpoints de importacao/integracao publica. O tenant continua vindo do JWT/path, nunca do body.
- `/reports/internal/**`: aceita apenas `X-Internal-Token` e fica bloqueado no gateway publico.

## Cards financeiros

- `gross_value`: faturado bruto.
- `received_value`: recebido/liberado.
- `fee_value`: taxas.
- `receivable_value`: a receber.
- `entry_count`: quantidade de lancamentos considerados.

## Nucleo Fiscal MVP

O nucleo fiscal da Fase 1 fica no `reporting-service`; nao foi criado um microservice novo porque a DRE depende diretamente do read model financeiro ja materializado, e o escopo atual ainda e cadastro simples/manual. Um `accounting-service` separado passa a fazer sentido quando houver apuracao fiscal propria, integracao NF-e/SPED, conciliacao bancaria ou workflow contabil independente.

Regimes aceitos:

- `SIMPLES_NACIONAL`
- `LUCRO_PRESUMIDO`
- `LUCRO_REAL`

O campo `estimated_tax_rate` usa decimal entre `0` e `1`; por exemplo, `0.0600` representa 6%.

Despesas exigem comprovante com metadados retornados pelo Cloudinary: `public_id`, `secure_url`, `resource_type`, `original_filename`, `content_type` e `size_bytes`. O backend nao usa AWS S3. Para upload seguro, o frontend chama `GET /reports/tenants/{tenantId}/expenses/upload-signature`, envia o arquivo direto ao Cloudinary com os parametros assinados e depois persiste a referencia segura na despesa. Despesas sem `public_id` e `secure_url` sao rejeitadas e nunca entram como deducao da DRE.

A DRE simplificada calcula:

- `gross_revenue`: soma de faturamento bruto dos lancamentos.
- `marketplace_fees`: soma das taxas importadas dos marketplaces.
- `estimated_taxes`: `gross_revenue * estimated_tax_rate`.
- `operating_expenses`: despesas manuais do periodo.
- `net_result`: `gross_revenue - marketplace_fees - estimated_taxes - operating_expenses`.

## Fechamento Contabil

O endpoint `POST /reports/tenants/{tenantId}/closings/{month}/sign` registra a assinatura digital do contador para o mes informado. A partir desse registro, `report_entries` e `expense_entries` daquele periodo ficam bloqueados nos repositórios antes de qualquer `INSERT`, `UPDATE`, `UPSERT` ou `DELETE`.

Cancelamentos retroativos nao devem reabrir o mes assinado: devem ser lancados como novo ajuste no mes corrente.

## Precisao Financeira

Campos monetarios persistidos em PostgreSQL usam `DECIMAL(10,2)`. `FLOAT`, `REAL` e `DOUBLE` nao devem ser usados para transacoes financeiras.

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
