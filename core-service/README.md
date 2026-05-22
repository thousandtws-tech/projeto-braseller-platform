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

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

## Build

```shell
./mvnw verify
```
