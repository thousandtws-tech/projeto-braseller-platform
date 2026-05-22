# Brasaller Sistema Modular

Baseline DevOps para os microservices Quarkus do projeto Brasaller.

## Servicos

| Servico | Porta dev | Porta Compose | Banco local | Health | Metrics | OpenAPI |
| --- | ---: | ---: | --- | --- | --- | --- |
| gateway-api | 8080 | 8080 | `localhost:5432/gateway_api` | `/q/health` | `/q/metrics` | `/q/openapi` |
| core-service | 8081 | 8081 | `localhost:5433/core_service` | `/q/health` | `/q/metrics` | `/q/openapi` |
| billing-service | 8082 | 8082 | `localhost:5434/billing_service` | `/q/health` | `/q/metrics` | `/q/openapi` |
| notification-service | 8083 | 8083 | `localhost:5435/notification_service` | `/q/health` | `/q/metrics` | `/q/openapi` |
| user-service | 8084 | 8084 | `localhost:5436/user_service` | `/q/health` | `/q/metrics` | `/q/openapi` |
| auth-service | 8085 | 8085 | `localhost:5437/auth_service` | `/q/health` | `/q/metrics` | `/q/openapi` |

## Modulo Core: Auth + User + Multi-tenant

Este modulo foi montado como a base reutilizavel da Fase 1. O `user-service` e a fonte de tenants, usuarios, papeis e acesso do contador. O `auth-service` delega cadastro/login ao `user-service`, sincroniza uma identidade minima no proprio banco e emite JWT/refresh tokens. O `core-service` valida o contexto tenant-aware que os demais modulos devem consumir.

Contratos principais:

- `POST /auth/register`: cria tenant + usuario admin/vendedor pelo `user-service` e retorna JWT.
- `POST /auth/login`: valida e-mail/senha no `user-service` e retorna JWT.
- `POST /auth/refresh` e `POST /auth/logout`: gerenciam sessoes no banco do `auth-service`.
- `GET /auth/oauth/google/authorize-url`: prepara o fluxo OAuth Google quando `GOOGLE_CLIENT_ID` estiver configurado.
- `POST /users/tenants/register`: cria tenant isolado e usuario admin inicial.
- `POST /users/tenants/{tenantId}/accountants`: cria acesso secundario de contador como `CONTADOR`, somente leitura.
- `GET /users/tenants/{tenantId}/members`: lista membros do tenant.
- `POST /users/internal/identity/verify-password`: contrato interno protegido por `X-Internal-Token`.
- `GET /core/context`: valida Bearer JWT e retorna `tenantId`, `userId`, `email`, `roles` e `readOnly`.

Claims padrao do JWT: `tenant_id`, `user_id`, `email`, `roles` e `groups`. Os papeis suportados sao `ADMIN`, `VENDEDOR` e `CONTADOR`; `CONTADOR` e tratado como leitura quando nao houver papel `ADMIN`.

Em ambientes fora de desenvolvimento, troque obrigatoriamente `AUTH_JWT_SECRET`, `INTERNAL_SERVICE_TOKEN`, senhas dos bancos e credenciais do Grafana.

## Verificacao local

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-services.ps1
```

## Subir ambiente local

```powershell
docker compose --env-file .env.example up --build
```

Prometheus fica em `http://localhost:9090`.
Grafana fica em `http://localhost:3001` com usuario `admin` e senha `admin` por padrao, configuraveis no `.env.example`.

Cada microservice possui um Postgres 17 isolado, com usuario, database e volume proprios. As migrations ficam em `src/main/resources/db/migration` dentro de cada servico e rodam automaticamente no startup via Flyway.

## Swagger / OpenAPI

Cada servico publica a especificacao OpenAPI em `/q/openapi` e o Swagger UI em `/q/swagger-ui` quando `SWAGGER_UI_ENABLED=true`. No Quarkus, o Swagger UI e incluido no artefato durante o build, entao o Compose tambem passa `SWAGGER_UI_ENABLED` como build arg.

Links locais pelo Compose:

- Gateway API: `http://localhost:8080/q/swagger-ui`
- Core Service: `http://localhost:8081/q/swagger-ui`
- Billing Service: `http://localhost:8082/q/swagger-ui`
- Notification Service: `http://localhost:8083/q/swagger-ui`
- User Service: `http://localhost:8084/q/swagger-ui`
- Auth Service: `http://localhost:8085/q/swagger-ui`

Em producao, faca o build com `SWAGGER_UI_ENABLED=false` e mantenha `/q/openapi` disponivel apenas conforme a politica de rede/seguranca do ambiente.

## Build de imagens

```powershell
powershell -ExecutionPolicy Bypass -File scripts/build-images.ps1 -Tag local
```

## Padroes aplicados

- Build Java 21 validado por Maven Enforcer.
- Health, readiness, metrics Prometheus e OpenAPI em todos os servicos.
- Database-per-service com PostgreSQL 17, Flyway e pool JDBC configuravel.
- Clean Architecture no modulo Core/Auth/User com `domain`, `application`, `infrastructure` e `interfaces.rest`.
- Grafana OSS provisionado com datasource Prometheus e dashboard dos microservices.
- Logs JSON em profile `prod`, logs legiveis em dev/test.
- Docker multi-stage, runtime sem root, healthcheck e limites no Compose.
- CI em matriz por servico com testes Maven e build de imagem.

Detalhes da arquitetura ficam em `docs/clean-architecture.md`.


Porta http://127.0.0.1:8083/
