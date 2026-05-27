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

Este modulo foi montado como a base reutilizavel da Fase 1. O `user-service` e a fonte de tenants, usuarios, papeis e acesso do contador. O `auth-service` usa o Keycloak para credenciais, sessoes, refresh, logout e OAuth, sincroniza o perfil no `user-service` e emite o JWT interno da plataforma com contexto multi-tenant. O `core-service` valida esse contexto tenant-aware que os demais modulos devem consumir.

Contratos principais:

- `POST /auth/register`: cria tenant + usuario admin/vendedor no `user-service`, cria o usuario no Keycloak e retorna JWT da plataforma.
- `POST /auth/login`: autentica e-mail/senha no Keycloak e retorna JWT da plataforma.
- `POST /auth/refresh` e `POST /auth/logout`: renovam/revogam sessao no Keycloak.
- `GET /auth/oauth/google/authorize-url`: prepara o fluxo Google pelo broker do Keycloak (`kc_idp_hint=google`).
- `POST /auth/oauth/google/callback`: finaliza login/cadastro Google pelo broker do Keycloak.
- `POST /users/tenants/register`: cria tenant isolado e usuario admin inicial.
- `POST /users/tenants/{tenantId}/accountants`: cria acesso secundario de contador como `CONTADOR`, somente leitura; exige JWT do mesmo tenant com papel `ADMIN`.
- `GET /users/tenants/{tenantId}/members`: lista membros do tenant resolvido pelo JWT.
- `POST /users/internal/identity/verify-password`: contrato interno protegido por `X-Internal-Token`.
- `POST /users/internal/identity/sync-profile`: contrato interno para persistir perfil do Keycloak, incluindo login via broker Google, no `user-service`.
- `GET /core/context`: valida Bearer JWT e retorna `tenantId`, `userId`, `email`, `roles` e `readOnly`.

Claims padrao do JWT: `tenant_id`, `user_id`, `email`, `roles` e `groups`. Os papeis suportados sao `ADMIN`, `VENDEDOR` e `CONTADOR`; `CONTADOR` e tratado como leitura quando nao houver papel `ADMIN`.

Endpoints tenant-aware de `user-service`, `core-service` e `notification-service` derivam o tenant do JWT. O cliente nao escolhe tenant por query/body/header. Acoes de escrita exigem `ADMIN` ou `VENDEDOR`; `CONTADOR` pode apenas consultar dados do proprio tenant. Eventos internos de notificacao usam `X-Internal-Token` e nao sao expostos pelo gateway publico.

Em ambientes fora de desenvolvimento, troque obrigatoriamente `AUTH_JWT_SECRET`, `INTERNAL_SERVICE_TOKEN`, senhas dos bancos e credenciais do Grafana.

## Core: Camada de Conectores

O `core-service` define a interface padronizada entre o Core e os conectores de marketplace. O Core nunca importa codigo de Mercado Livre, Shopee, Amazon ou qualquer marketplace especifico; ele resolve um conector pelo nome e recebe sempre os mesmos modelos.

Contrato obrigatorio de todo conector:

- `authenticate()`: token de acesso do usuario.
- `refreshToken()`: token renovado automaticamente.
- `getOrders(filtros)`: lista padronizada de pedidos.
- `getOrderDetail(id)`: detalhes completos de um pedido.
- `getPayments(orderId)`: dados de pagamento e liberacao.
- `getFees(orderId)`: taxas e comissoes.
- `syncAll(desde)`: sincronizacao completa desde uma data.
- `getStatus()`: status da conexao.

Contrato opcional:

- `getInvoices(filtros)`: notas fiscais quando a plataforma disponibilizar.

Endpoints do Core:

- `GET /core/connectors`: lista conectores registrados.
- `POST /core/connectors/{connectorName}/authenticate`.
- `POST /core/connectors/{connectorName}/refresh-token`.
- `GET /core/connectors/{connectorName}/orders`.
- `GET /core/connectors/{connectorName}/orders/{orderId}`.
- `GET /core/connectors/{connectorName}/orders/{orderId}/payments`.
- `GET /core/connectors/{connectorName}/orders/{orderId}/fees`.
- `GET /core/connectors/{connectorName}/invoices`.
- `POST /core/connectors/{connectorName}/sync-all`.
- `GET /core/connectors/{connectorName}/status`.

O conector `sandbox` foi incluido para validar o contrato sem depender de marketplace real. Novos marketplaces entram como adapters que implementam a porta `MarketplaceConnector`.

## Modulo Notification

O `notification-service` centraliza comunicacao com o usuario, alertas operacionais e e-mails automaticos.

Funcionalidades iniciais:

- E-mail automatico de fechamento mensal com resumo.
- Alerta quando pagamento do Mercado Livre esta proximo de liberar.
- Notificacao de nova venda opcional, ativavel pelo usuario.
- Relatorio semanal automatico enviado ao contador.
- Preferencias por tenant para canais e tipos de notificacao.

Contratos principais:

- `GET /notifications/tenants/{tenantId}/preferences`: consulta preferencias.
- `PUT /notifications/tenants/{tenantId}/preferences`: atualiza preferencias.
- `GET /notifications/tenants/{tenantId}`: lista notificacoes nao arquivadas.
- `PATCH /notifications/tenants/{tenantId}/{notificationId}/read`: marca notificacao como lida.
- `POST /notifications/events/new-sale`: cria notificacao de nova venda quando habilitada.
- `POST /notifications/events/ml-payment-release`: cria alerta de pagamento ML proximo de liberar.
- `POST /notifications/events/monthly-closing`: envia fechamento mensal.
- `POST /notifications/events/weekly-accountant-report`: envia relatorio semanal ao contador.

Consultas em `/notifications/tenants/{tenantId}/...` exigem Bearer JWT do mesmo tenant. Atualizacoes exigem papel de escrita. Endpoints `/notifications/events/**` sao service-to-service, protegidos por `X-Internal-Token`, e bloqueados em `/api/notifications/events/**` pelo gateway.

## Mensageria Kafka

O ambiente Docker sobe um broker Kafka local em `localhost:9092` e cria os topicos versionados usados pelos microservices.

- `core-service` publica eventos de nova venda em `brasaller.notifications.new-sale.v1` ao executar `POST /core/connectors/{connectorName}/sync-all`.
- `notification-service` consome o topico com o grupo `notification-service` e cria a notificacao conforme as preferencias do tenant.
- `notification-service` tambem executa Kafka Streams sobre o mesmo topico, mantendo a KTable `tenant-new-sale-summary-store` com resumo de vendas por tenant.
- A KTable publica atualizacoes no topico compactado `brasaller.analytics.tenant-new-sale-summary.v1` e pode ser consultada em `GET /notifications/tenants/{tenantId}/new-sale-summary`.
- Falhas de processamento sao enviadas para `brasaller.notifications.new-sale.dlq.v1`.

Variaveis principais: `KAFKA_BOOTSTRAP_SERVERS`, `BRASALLER_KAFKA_TOPIC_NOTIFICATION_NEW_SALE`, `BRASALLER_KAFKA_TOPIC_NOTIFICATION_NEW_SALE_DLQ`, `BRASALLER_KAFKA_TOPIC_NOTIFICATION_NEW_SALE_SUMMARY`, `KAFKA_NOTIFICATION_GROUP_ID`, `KAFKA_STREAMS_APPLICATION_ID` e `KAFKA_STREAMS_APPLICATION_SERVER`. No Compose, os servicos usam `kafka:9093` pela rede interna Docker.

## Gateway API

O `gateway-api` publica a fachada HTTP em `/api` e encaminha chamadas para os microservices por Quarkus REST Client:

- `/api/auth/**` -> `auth-service` em `/auth/**`.
- `/api/users/**` -> `user-service` em `/users/**`, com `/users/internal/**` bloqueado no gateway.
- `/api/core/**` -> `core-service` em `/core/**`.
- `/api/billing/**` -> `billing-service` em `/billing/**`.
- `/api/notifications/**` -> `notification-service` em `/notifications/**`, com `/notifications/events/**` bloqueado no gateway.

Headers propagados: `Authorization`, `X-Tenant-Id`, `X-Request-Id`, `Accept` e `Content-Type`. As URLs downstream usam `AUTH_SERVICE_URL`, `USER_SERVICE_URL`, `CORE_SERVICE_URL`, `BILLING_SERVICE_URL` e `NOTIFICATION_SERVICE_URL`; no Compose elas ja apontam para a rede interna Docker.

## Verificacao local

```powershell
powershell -ExecutionPolicy Bypass -File scripts/verify-services.ps1
```

## Subir ambiente local

```powershell
docker compose --env-file .env.example up --build
```

Keycloak fica em `http://localhost:8086` com usuario `admin` e senha `admin` por padrao, configuraveis no `.env.example`.
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

O Swagger UI do Gateway API tambem mostra um seletor com as specs dos microservices. As URLs desse seletor sao configuradas por `AUTH_SERVICE_OPENAPI_URL`, `USER_SERVICE_OPENAPI_URL`, `CORE_SERVICE_OPENAPI_URL`, `BILLING_SERVICE_OPENAPI_URL` e `NOTIFICATION_SERVICE_OPENAPI_URL`; por padrao elas usam `localhost` porque quem baixa as specs e o navegador.

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
Desenvolvedor Responsalvel: Vinicius Moreira
