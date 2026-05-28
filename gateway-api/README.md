# gateway-api

Gateway HTTP dos microservices BraSeller usando Quarkus REST e Quarkus REST Client.

## Rotas publicas

| Gateway | Downstream |
| --- | --- |
| `GET /api` | Lista as rotas configuradas |
| `/api/auth/**` | `auth-service` em `/auth/**` |
| `/api/users/**` | `user-service` em `/users/**` |
| `/api/core/**` | `core-service` em `/core/**` |
| `/api/billing/**` | `billing-service` em `/billing/**` |
| `/api/notifications/**` | `notification-service` em `/notifications/**` |
| `/api/reports/**` | `reporting-service` em `/reports/**` |

O gateway repassa `Authorization`, `X-Tenant-Id`, `X-Request-Id`, `Accept`, `Content-Type` e `X-Billing-Webhook-Token`. Endpoints internos do `user-service`, como `/users/internal/**`, eventos internos do `notification-service`, como `/notifications/events/**`, e ingestao interna do `reporting-service`, como `/reports/internal/**`, ficam bloqueados no gateway.

## Configuracao

```properties
AUTH_SERVICE_URL=http://localhost:8085
USER_SERVICE_URL=http://localhost:8084
CORE_SERVICE_URL=http://localhost:8081
BILLING_SERVICE_URL=http://localhost:8082
NOTIFICATION_SERVICE_URL=http://localhost:8083
REPORTING_SERVICE_URL=http://localhost:8087
GATEWAY_DOWNSTREAM_CONNECT_TIMEOUT_MS=2000
GATEWAY_DOWNSTREAM_READ_TIMEOUT_MS=30000
```

No Compose, essas URLs apontam para os nomes dos servicos na rede Docker. Em dev mode, os defaults usam as portas locais do README raiz. O timeout de leitura fica em 30s para cobrir fluxos de cadastro que chamam `auth-service`, `user-service` e Keycloak em sequencia.

## Swagger UI

O Swagger UI do gateway fica em:

```text
http://localhost:8080/q/swagger-ui
```

Ele abre com a spec do `gateway-api` e inclui um seletor para as specs dos microservices:

```properties
AUTH_SERVICE_OPENAPI_URL=http://localhost:8085/q/openapi
USER_SERVICE_OPENAPI_URL=http://localhost:8084/q/openapi
CORE_SERVICE_OPENAPI_URL=http://localhost:8081/q/openapi
BILLING_SERVICE_OPENAPI_URL=http://localhost:8082/q/openapi
NOTIFICATION_SERVICE_OPENAPI_URL=http://localhost:8083/q/openapi
REPORTING_SERVICE_OPENAPI_URL=http://localhost:8087/q/openapi
```

Essas URLs precisam ser acessiveis pelo navegador, por isso usam `localhost` no ambiente local mesmo quando o gateway roda em container.

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

## Testes

```shell
./mvnw test
```
