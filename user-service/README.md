# user-service

Microservice Quarkus responsavel por tenants, usuarios, papeis e acesso secundario do contador.

## Endpoints locais

- API: `http://localhost:8084/users`
- Health: `http://localhost:8084/q/health`
- Metrics: `http://localhost:8084/q/metrics`
- OpenAPI: `http://localhost:8084/q/openapi`
- Swagger UI: `http://localhost:8084/q/swagger-ui`

## Contratos

- `POST /users/tenants/register`: cria tenant isolado e usuario admin inicial com papeis `ADMIN` e `VENDEDOR`.
- `POST /users/tenants/{tenantId}/accountants`: cria usuario contador convidado, com papel `CONTADOR` e acesso somente leitura.
- `GET /users/tenants/{tenantId}/members`: lista usuarios e papeis do tenant.
- `POST /users/internal/identity/verify-password`: endpoint interno usado pelo `auth-service`, protegido por `X-Internal-Token`.

O schema usa indices em colunas de tenant, status e chaves estrangeiras para manter as consultas multi-tenant previsiveis com crescimento de dados.

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

## Build

```shell
./mvnw verify
```
