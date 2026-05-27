# auth-service

Microservice Quarkus responsavel por autenticacao, emissao de JWT da plataforma e fachada HTTP para o Keycloak.

O servico usa o `user-service` como fonte de tenants, usuarios e papeis, mas credenciais, sessoes, refresh token, logout e OAuth passam pelo Keycloak. O `accessToken` retornado continua sendo o JWT interno da plataforma com `tenant_id`, `user_id` e `roles`, para os outros microservices validarem o isolamento multi-tenant.

## Endpoints locais

- API: `http://localhost:8085/auth`
- Health: `http://localhost:8085/q/health`
- Metrics: `http://localhost:8085/q/metrics`
- OpenAPI: `http://localhost:8085/q/openapi`
- Swagger UI: `http://localhost:8085/q/swagger-ui`

## Contratos

- `POST /auth/register`: cria tenant/usuario no `user-service`, cria o usuario no Keycloak e emite JWT da plataforma com refresh token Keycloak.
- `POST /auth/login`: autentica e-mail/senha no Keycloak e emite tokens.
- `POST /auth/refresh`: renova a sessao no Keycloak e emite novo access token da plataforma.
- `POST /auth/logout`: revoga o refresh token no Keycloak.
- `GET /auth/oauth/google/authorize-url`: retorna a URL do Keycloak com `kc_idp_hint=google`.
- `POST /auth/oauth/google/callback`: troca no Keycloak o `code` recebido pelo broker Google. Se o e-mail ja existir, faz login; se nao existir, cria tenant/usuario usando `tenantName`.

Variaveis importantes: `USER_SERVICE_URL`, `INTERNAL_SERVICE_TOKEN`, `AUTH_JWT_SECRET`, `AUTH_JWT_ISSUER`, `AUTH_JWT_AUDIENCE`, `KEYCLOAK_BASE_URL`, `KEYCLOAK_PUBLIC_BASE_URL`, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET`, `KEYCLOAK_REDIRECT_URI`, `KEYCLOAK_ADMIN_USERNAME` e `KEYCLOAK_ADMIN_PASSWORD`.

## OAuth

No Keycloak, use o realm `brasaller` e um client OpenID Connect com Standard Flow e Direct Access Grants habilitados. No ambiente Docker deste repositorio, o realm de desenvolvimento e importado de `keycloak/realm-brasaller.json`, com eventos/admin events ligados, o client publico `auth-service` e callback local `http://localhost:3000/auth/callback`.

Para um Keycloak externo, preencha `KEYCLOAK_BASE_URL`, `KEYCLOAK_PUBLIC_BASE_URL` quando a URL do navegador for diferente da URL interna do auth-service, `KEYCLOAK_REALM`, `KEYCLOAK_CLIENT_ID`, `KEYCLOAK_CLIENT_SECRET` se o client for confidencial, e `KEYCLOAK_REDIRECT_URI`.

O realm local tambem ja importa o provider Google com alias `google`. Para habilitar de verdade, crie um OAuth Client ID do tipo Web application no Google Cloud e use como Authorized redirect URI:

```text
http://localhost:8086/realms/brasaller/broker/google/endpoint
```

Depois preencha `KEYCLOAK_GOOGLE_CLIENT_ID` e `KEYCLOAK_GOOGLE_CLIENT_SECRET`. As opcoes `Use userIp param`, `Request refresh token` e `JWT Authorization Grant` ficam desligadas por padrao no import.

O login/cadastro OAuth vincula a identidade pelo e-mail. Para primeiro cadastro, envie `tenantName` no callback; depois disso o mesmo e-mail entra sem precisar informar tenant.

Em todo login externo, o `auth-service` sincroniza o perfil no `user-service` por `POST /users/internal/identity/sync-profile`, mantendo nome, usuario preferido, nome/sobrenome, foto, provider e subject atualizados na fonte de usuarios.

As respostas de `POST /auth/login`, `POST /auth/register` e callbacks OAuth retornam tambem `profile`, com os dados do usuario autenticado:

```json
{
  "email": "cliente@empresa.com",
  "roles": ["ADMIN", "VENDEDOR"],
  "profile": {
    "provider": "KEYCLOAK",
    "subject": "keycloak-user-id",
    "email": "cliente@empresa.com",
    "fullName": "Cliente Empresa",
    "preferredUsername": "cliente@empresa.com",
    "firstName": "Cliente",
    "lastName": "Empresa",
    "pictureUrl": "https://...",
    "emailVerified": true,
    "roles": ["ADMIN", "VENDEDOR"]
  }
}
```

Payload do callback Google:

```json
{
  "code": "codigo-retornado-pelo-keycloak-google-broker",
  "tenantName": "Minha Empresa"
}
```

Para login posterior, `tenantName` pode ser omitido.

## Desenvolvimento

```shell
./mvnw quarkus:dev
```

## Build

```shell
./mvnw verify
```
