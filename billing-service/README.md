# billing-service

Microservice Quarkus responsavel por planos, trials, assinaturas e eventos de cobranca do BraSeller.

A integracao real com Stripe ou Pagar.me ainda nao foi implementada. O servico ja deixa essa fronteira isolada na porta `BillingProviderGateway` e usa um adapter `LOCAL` para validar o fluxo enquanto o provedor definitivo nao entra.

## Endpoints locais

- API: `http://localhost:8082/billing`
- Health: `http://localhost:8082/q/health`
- Metrics: `http://localhost:8082/q/metrics`
- OpenAPI: `http://localhost:8082/q/openapi`
- Swagger UI: `http://localhost:8082/q/swagger-ui`

## Contratos

- `GET /billing`: status do servico.
- `GET /billing/plans`: lista planos `BASIC`, `PRO` e `AGENCY`.
- `GET /billing/tenants/{tenantId}/subscription`: consulta assinatura atual do tenant.
- `POST /billing/tenants/{tenantId}/trial`: inicia trial gratuito de 14 dias.
- `PUT /billing/tenants/{tenantId}/subscription/plan`: upgrade ou downgrade pelo usuario.
- `POST /billing/webhooks`: aplica eventos de cobranca do provedor.

## Planos

- `BASIC`: plano inicial para um marketplace.
- `PRO`: plano multi-marketplace para operacao recorrente.
- `AGENCY`: plano para agencias e contadores com maior limite operacional.

Todos os planos iniciam com `trial_days = 14`.

## Webhooks

Enquanto Stripe/Pagar.me nao entram, os webhooks aceitam eventos normalizados:

- `SUBSCRIPTION_ACTIVATED`
- `PAYMENT_SUCCEEDED`
- `PAYMENT_FAILED`
- `SUBSCRIPTION_SUSPENDED`
- `SUBSCRIPTION_CANCELLED`

O endpoint exige `X-Billing-Webhook-Token`, configurado por `BILLING_WEBHOOK_TOKEN`.

## Controle de acesso

Consultas de assinatura exigem Bearer JWT do mesmo tenant.

- `ADMIN`, `VENDEDOR` e `CONTADOR`: podem consultar.
- `ADMIN` e `VENDEDOR`: podem iniciar trial e alterar plano.
- `CONTADOR`: somente leitura.

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

## Build

```shell
./mvnw verify
```
