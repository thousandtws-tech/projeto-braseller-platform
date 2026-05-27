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
- `POST /users/tenants/{tenantId}/accountants`: cria usuario contador convidado, com papel `CONTADOR` e acesso somente leitura. Exige Bearer JWT do mesmo tenant com papel `ADMIN`; o usuario concedente vem do JWT.
- `GET /users/tenants/{tenantId}/members`: lista usuarios e papeis do tenant. Exige Bearer JWT do mesmo tenant com papel `ADMIN`, `VENDEDOR` ou `CONTADOR`.
- `POST /users/internal/identity/verify-password`: endpoint interno usado pelo `auth-service`, protegido por `X-Internal-Token`.
- `POST /users/internal/identity/sync-profile`: endpoint interno usado pelo `auth-service` para persistir perfil vindo de Keycloak/Google.

O `tenantId` do path precisa bater com o claim `tenant_id` do JWT. Headers ou campos enviados pelo cliente nao autorizam acesso cross-tenant.

Os usuarios guardam campos de perfil sincronizados do provedor externo: `preferredUsername`, `firstName`, `lastName`, `pictureUrl`, `emailVerified`, `provider` e `providerSubject`. Esses campos aparecem em `UserView` e `IdentityVerification`.

O schema usa indices em colunas de tenant, status e chaves estrangeiras para manter as consultas multi-tenant previsiveis com crescimento de dados.

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

## Build

```shell
./mvnw verify
```
