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

## Eventos Kafka

Ao executar `POST /core/connectors/{connectorName}/sync-all`, o servico publica eventos de nova venda no topico `brasaller.notifications.new-sale.v1` usando `KAFKA_BOOTSTRAP_SERVERS`. O `notification-service` consome esse evento para criar notificacoes sem chamada HTTP service-to-service.

Os endpoints tenant-aware do Core resolvem `tenantId` exclusivamente a partir do Bearer JWT. Nenhum endpoint de conector usa `tenantId` recebido por query string ou body como autoridade de isolamento.

## Camada de conectores

O Core define o contrato padronizado para conectores de marketplace. Ele nao importa codigo de Mercado Livre, Shopee, Amazon ou qualquer plataforma especifica; ele resolve um conector pelo nome e sempre recebe os mesmos DTOs.

Conector de validacao disponivel:

- `sandbox`: implementacao local para exercitar o contrato completo sem depender de marketplace real.

Endpoints:

| Metodo | Rota | Contrato |
| --- | --- | --- |
| `GET` | `/core/connectors` | Lista conectores registrados |
| `POST` | `/core/connectors/{connectorName}/authenticate` | `authenticate()` |
| `POST` | `/core/connectors/{connectorName}/refresh-token` | `refreshToken()` |
| `GET` | `/core/connectors/{connectorName}/orders` | `getOrders(filtros)` com `from`, `to`, `status=paid|pending|cancelled` e `limit` |
| `GET` | `/core/connectors/{connectorName}/orders/{orderId}` | `getOrderDetail(id)` |
| `GET` | `/core/connectors/{connectorName}/orders/{orderId}/payments` | `getPayments(orderId)` |
| `GET` | `/core/connectors/{connectorName}/orders/{orderId}/fees` | `getFees(orderId)` |
| `GET` | `/core/connectors/{connectorName}/invoices` | `getInvoices(filtros)` opcional |
| `POST` | `/core/connectors/{connectorName}/sync-all` | `syncAll(desde)` |
| `GET` | `/core/connectors/{connectorName}/status` | `getStatus()` |

Todos os endpoints de conector, exceto a listagem de conectores registrados, exigem Bearer JWT. Consultas aceitam `ADMIN`, `VENDEDOR` ou `CONTADOR`; autenticacao, refresh e sincronizacao exigem `ADMIN` ou `VENDEDOR`.

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

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

## Build

```shell
./mvnw verify
```
