# auth-service

Microservice Quarkus responsavel por autenticacao, emissao de JWT e sessoes.

O servico usa o `user-service` como fonte de identidade para cadastro e login. O banco local do `auth-service` guarda uma copia minima da identidade para refresh de sessao, alem de `auth_sessions` com refresh token hasheado.

## Endpoints locais

- API: `http://localhost:8085/auth`
- Health: `http://localhost:8085/q/health`
- Metrics: `http://localhost:8085/q/metrics`
- OpenAPI: `http://localhost:8085/q/openapi`
- Swagger UI: `http://localhost:8085/q/swagger-ui`

## Contratos

- `POST /auth/register`: cria tenant e usuario admin/vendedor no `user-service` e emite tokens.
- `POST /auth/login`: valida e-mail/senha no `user-service` e emite tokens.
- `POST /auth/refresh`: emite novo access token a partir do refresh token valido.
- `POST /auth/logout`: revoga o refresh token.
- `GET /auth/oauth/google/authorize-url`: retorna a URL de autorizacao do Google quando configurado.

Variaveis importantes: `USER_SERVICE_URL`, `INTERNAL_SERVICE_TOKEN`, `AUTH_JWT_SECRET`, `AUTH_JWT_ISSUER`, `AUTH_JWT_AUDIENCE`, `GOOGLE_CLIENT_ID` e `GOOGLE_REDIRECT_URI`.

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

## Build

```shell
./mvnw verify
```
