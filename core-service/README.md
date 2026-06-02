# core-service

Microservice Quarkus responsavel pelo contexto compartilhado do BraSeller: validacao de tenant, usuario, papeis e trilha de auditoria basica.

## Endpoints locais

- API: `http://localhost:8081/core`
- Health: `http://localhost:8081/q/health`
- Metrics: `http://localhost:8081/q/metrics`
- OpenAPI: `http://localhost:8081/q/openapi`
- Swagger UI: `http://localhost:8081/q/swagger-ui`

## Contratos

- `GET /core/context`: valida o Bearer JWT emitido pelo `auth-service` e retorna `tenantId`, `userId`, `email`, `roles` e `readOnly`.

O contexto espera JWT HS256 com `AUTH_JWT_SECRET`, `AUTH_JWT_ISSUER` e `AUTH_JWT_AUDIENCE` iguais aos do `auth-service`. O papel `CONTADOR` sem `ADMIN` e tratado como somente leitura.

## Processamento interno

Ao executar `POST /core/connectors/{connectorName}/sync-all`, o servico cria um job em `connector_sync_jobs`, grava a solicitacao no outbox interno e responde `202 Accepted` com `job_id`. Um scheduler do proprio Core processa o job em background, e o status pode ser consultado em `GET /core/connectors/sync-jobs/{jobId}`.

Depois da sincronizacao, o Core usa chamadas REST internas protegidas por `X-Internal-Token` para:

- Enviar lancamentos normalizados para `POST /reports/internal/entries`.
- Enviar eventos de nova venda para `POST /notifications/events/new-sale`.

Falhas ficam registradas no outbox interno para retry controlado e no status do job de sincronizacao.

Os endpoints tenant-aware do Core resolvem `tenantId` exclusivamente a partir do Bearer JWT. Nenhum endpoint de conector usa `tenantId` recebido por query string ou body como autoridade de isolamento.

## Camada de conectores

O Core define o contrato padronizado para conectores de marketplace. Ele nao importa codigo de Mercado Livre, Shopee, Amazon ou qualquer plataforma especifica; ele resolve um conector pelo nome e sempre recebe os mesmos DTOs.

Conector de validacao disponivel:

- `sandbox`: implementacao local para exercitar o contrato completo sem depender de marketplace real.
- `mercado-livre`: adapter MVP para MELI API com OAuth 2.0, pedidos, pagamentos, taxas e refresh automatico de token.

Endpoints:

| Metodo | Rota | Contrato |
| --- | --- | --- |
| `GET` | `/core/connectors` | Lista conectores registrados |
| `POST` | `/core/connectors/{connectorName}/authenticate` | `authenticate()` |
| `POST` | `/core/connectors/{connectorName}/refresh-token` | `refreshToken()` usando o refresh token criptografado no Core |
| `GET` | `/core/connectors/{connectorName}/orders` | `getOrders(filtros)` com `from`, `to`, `status=paid|pending|cancelled` e `limit` |
| `GET` | `/core/connectors/{connectorName}/orders/{orderId}` | `getOrderDetail(id)` |
| `GET` | `/core/connectors/{connectorName}/orders/{orderId}/payments` | `getPayments(orderId)` |
| `GET` | `/core/connectors/{connectorName}/orders/{orderId}/fees` | `getFees(orderId)` |
| `GET` | `/core/connectors/{connectorName}/invoices` | `getInvoices(filtros)` opcional |
| `POST` | `/core/connectors/{connectorName}/sync-all` | Enfileira `syncAll(desde)` para processamento assincrono |
| `GET` | `/core/connectors/sync-jobs/{jobId}` | Consulta status/resultado do job de sincronizacao |
| `GET` | `/core/connectors/{connectorName}/status` | `getStatus()` |

Todos os endpoints de conector, exceto a listagem de conectores registrados, exigem Bearer JWT. Consultas aceitam `ADMIN`, `VENDEDOR` ou `CONTADOR`; autenticacao, refresh e sincronizacao exigem `ADMIN` ou `VENDEDOR`. Respostas HTTP nunca expõem `access_token` ou `refresh_token` de marketplace.

Formato padronizado de pedido:

```json
{
  "order_id": "SANDBOX-1001",
  "platform": "sandbox",
  "date": "2026-05-21",
  "gross_value": 199.90,
  "platform_fee": 26.40,
  "net_value": 173.50,
  "payment_method": "PIX",
  "payment_date": "2026-05-21",
  "release_date": "2026-06-04",
  "status": "paid",
  "buyer_name": "Comprador Sandbox",
  "items": [],
  "invoice_number": "NF-SANDBOX-1001"
}
```

Para adicionar um novo marketplace, crie um adapter em `infrastructure.connector` implementando `MarketplaceConnector`. A camada `application` continua falando apenas com essa porta. `getInvoices()` e opcional no contrato: conectores sem NF podem usar o default vazio e declarar `supports_invoices=false` no descriptor.

## Conector Mercado Livre

Variaveis necessarias para habilitar o OAuth:

```shell
MERCADOLIVRE_CLIENT_ID=
MERCADOLIVRE_CLIENT_SECRET=
MERCADOLIVRE_REDIRECT_URI=
MERCADOLIVRE_REFRESH_SKEW_SECONDS=300
CONNECTOR_TOKEN_ENCRYPTION_KEY=
```

Fluxo de uso:

1. Cadastre o app em `developers.mercadolivre.com.br` com a mesma `redirect_uri`.
2. Apos receber o `code` OAuth, chame `POST /core/connectors/mercado-livre/authenticate`.
3. O Core salva `access_token`, `refresh_token`, `seller_id` e vencimento por tenant; os tokens ficam criptografados com AES-256 no banco.
4. Chamadas de leitura usam `/orders/search`, `/orders/{id}`, `/payments/{id}` e `/users/{id}` via `https://api.mercadolibre.com`.
5. Antes de expirar, o conector troca o refresh token automaticamente e persiste o novo par de tokens criptografado.

O frontend envia apenas o `code` OAuth e nunca trafega tokens da plataforma diretamente. `POST /refresh-token` nao recebe token no body; ele usa o segredo persistido no Core.

Datas retornadas em UTC pela MELI API sao normalizadas para `America/Sao_Paulo` no formato padrao do Core. Taxas normalizadas somam `sale_fee` e `shipping_cost`.

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

## Build

```shell
./mvnw verify
```
