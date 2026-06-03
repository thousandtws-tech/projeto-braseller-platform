# Segurança e Integrações Externas

## Modelo de Segurança

### Camadas de Proteção

```mermaid
graph TB
    subgraph "Camada 1 — Transport"
        TLS[TLS 1.3\nHTTPS em todos os endpoints]
    end

    subgraph "Camada 2 — Autenticação"
        JWT[JWT RS256\nissuer: brasaller-auth\nautience: brasaller-platform\nTTL: 15 min]
        KC[Keycloak\nOAuth2/OIDC]
        ITOKEN[X-Internal-Token\nService-to-Service]
        WTOKEN[X-Billing-Webhook-Token\nWebhooks externos]
    end

    subgraph "Camada 3 — Autorização"
        TENANT[Tenant Isolation\ntenant_id obrigatório em\ntodas as queries]
        RBAC[RBAC\nADMIN / VENDEDOR / CONTADOR]
    end

    subgraph "Camada 4 — Dados"
        AES[AES-256\nTokens de marketplace]
        PBKDF2[PBKDF2\nHashes de senha]
    end

    TLS --> JWT
    TLS --> ITOKEN
    JWT --> TENANT
    JWT --> RBAC
    KC --> JWT
```

---

### Fluxo de Autorização por Role

```mermaid
graph LR
    subgraph "Roles"
        ADMIN[ADMIN\nDono da conta]
        VENDEDOR[VENDEDOR\nOperacional]
        CONTADOR[CONTADOR\nAcesso contábil]
    end

    subgraph "Permissões"
        ALL[Todas as funcionalidades]
        ORDERS[Pedidos, Sincronização\nDashboard financeiro]
        REPORTS[Relatórios, DRE\nFechamento, Exportação]
    end

    ADMIN --> ALL
    VENDEDOR --> ORDERS
    CONTADOR --> REPORTS
```

---

## Integrações Externas

### 1. Keycloak (Identity Provider)

| Aspecto | Detalhe |
|---------|---------|
| **Protocolo** | OAuth2 / OIDC |
| **Flows usados** | Resource Owner Password, Authorization Code (Google), Admin API |
| **Uso no auth-service** | Login, register, refresh, Google OAuth broker |
| **Uso no user-service** | Admin API para criar/gerenciar usuários e roles |
| **TTL token** | Configurável (padrão Keycloak) |
| **Realm** | `brasaller` (dedicado) |

```mermaid
sequenceDiagram
    participant AS as Auth Service
    participant KC as Keycloak
    participant US as User Service

    Note over AS,KC: Login
    AS->>KC: POST /realms/brasaller/protocol/openid-connect/token
    KC-->>AS: { access_token, refresh_token }

    Note over US,KC: Criar usuário (Admin API)
    US->>KC: POST /admin/realms/brasaller/users
    US->>KC: POST /admin/realms/brasaller/users/{id}/role-mappings
```

---

### 2. Mercado Livre API

| Aspecto | Detalhe |
|---------|---------|
| **Auth** | OAuth2 Authorization Code |
| **Tokens** | access_token (6h) + refresh_token (longo prazo), AES-256 encrypted no DB |
| **Endpoints consumidos** | /orders, /payments, /invoices, /fees |
| **Connector** | `MercadoLibreConnector` (implementa `MarketplaceConnector`) |
| **Normalização** | Transforma resposta ML → `StandardOrder` |

```mermaid
graph LR
    CS[Core Service\nMercadoLibreConnector]
    ML_AUTH[ML Auth\napi.mercadolibre.com/oauth/token]
    ML_ORDERS[ML Orders\napi.mercadolibre.com/orders/search]
    ML_PAY[ML Payments\napi.mercadolibre.com/collections/notifications]

    CS -->|OAuth2 code exchange| ML_AUTH
    CS -->|GET| ML_ORDERS
    CS -->|GET| ML_PAY
```

---

### 3. Cloudinary (Armazenamento de Anexos)

| Aspecto | Detalhe |
|---------|---------|
| **Uso** | Upload de comprovantes/recibos de despesas |
| **Auth** | API Key + Secret (signed uploads) |
| **Endpoint** | `GET /reports/tenants/{id}/expenses/upload-signature` retorna assinatura temporária |
| **Fluxo** | Frontend faz upload direto para Cloudinary usando assinatura do backend |

```mermaid
sequenceDiagram
    participant FE as Frontend
    participant RS as Reporting Service
    participant CDN as Cloudinary

    FE->>RS: GET /expenses/upload-signature
    RS->>RS: Gera timestamp + signature
    RS-->>FE: { signature, apiKey, cloudName, timestamp }
    FE->>CDN: POST /upload { file, signature, apiKey }
    CDN-->>FE: { url, publicId }
    FE->>RS: POST /expenses { amount, ..., attachmentUrl }
```

---

### 4. Clicksign (Assinatura Digital)

| Aspecto | Detalhe |
|---------|---------|
| **Uso** | Assinatura de fechamentos contábeis mensais |
| **Auth** | API Key via Authorization header |
| **Webhooks** | `POST /reports/webhooks/clicksign` recebe eventos de assinatura |
| **Fluxo** | Backend envia PDF → Clicksign → Contador assina → Webhook confirma |

---

### 5. Email Provider (Resend / SendGrid)

| Aspecto | Detalhe |
|---------|---------|
| **Framework** | Quarkus Mailer (SMTP / API) |
| **Templates** | Qute (HTML templating) |
| **Emails enviados** | Nova venda, Liberação de pagamento ML, Fechamento mensal, Relatório semanal contador |
| **Configuração** | `MAILER_HOST`, `MAILER_PORT`, `MAILER_USER`, `MAILER_PASSWORD` |

---

## Outbox Pattern — Garantia de Entrega de Eventos

```mermaid
graph TB
    subgraph "Core Service"
        CS_SVC[ConnectorService]
        OB[(outbox_events\nPENDING)]
        DISP[Dispatcher\nScheduler 5s]
    end

    subgraph "Targets"
        RS[Reporting Service\nPOST /reports/internal/entries]
        NS[Notification Service\nPOST /notifications/events/new-sale]
    end

    CS_SVC -->|INSERT| OB
    DISP -->|SELECT WHERE processed_at IS NULL| OB
    DISP -->|HTTP POST| RS
    DISP -->|HTTP POST| NS
    DISP -->|UPDATE processed_at| OB

    style OB fill:#336791,color:#fff
    style DISP fill:#E8A838,color:#000
```

**Garantias:**
- Atomicidade: insert na tabela de negócio + outbox na mesma transaction
- At-least-once delivery: retry em caso de falha HTTP
- Idempotência: targets devem tolerar duplicatas (upsert por `order_id + platform`)

---

## Variáveis de Ambiente por Serviço

### Variáveis Comuns (todos os serviços)

| Variável | Descrição | Exemplo |
|----------|-----------|---------|
| `HTTP_PORT` | Porta do serviço | `8080` |
| `DB_JDBC_URL` | JDBC connection string | `jdbc:postgresql://neon.tech/auth` |
| `DB_USERNAME` | Usuário do banco | `auth-service` |
| `DB_PASSWORD` | Senha do banco | `(secret)` |
| `LOG_LEVEL` | Nível de log | `INFO` |
| `LOG_JSON` | Log em JSON (produção) | `true` |
| `CORS_ORIGINS` | Origens CORS permitidas | `https://app.brasaller.com.br` |
| `SWAGGER_UI_ENABLED` | Habilita Swagger UI | `false` (prod) |

### Auth Service

| Variável | Descrição |
|----------|-----------|
| `AUTH_JWT_SECRET` | Segredo de assinatura JWT (min 256 bits) |
| `AUTH_JWT_TTL_SECONDS` | TTL do access token (padrão: 900) |
| `KEYCLOAK_BASE_URL` | URL base do Keycloak |
| `KEYCLOAK_REALM` | Realm Keycloak |
| `KEYCLOAK_CLIENT_ID` | Client ID OAuth2 |
| `KEYCLOAK_CLIENT_SECRET` | Client Secret OAuth2 |
| `INTERNAL_SERVICE_TOKEN` | Token para chamadas internas |
| `USER_SERVICE_URL` | URL do user-service |

### Core Service

| Variável | Descrição |
|----------|-----------|
| `ML_CLIENT_ID` | App ID Mercado Livre |
| `ML_CLIENT_SECRET` | App Secret Mercado Livre |
| `ML_REDIRECT_URI` | OAuth redirect URI |
| `TOKEN_ENCRYPTION_KEY` | Chave AES-256 para tokens |
| `REPORTING_SERVICE_URL` | URL do reporting-service |
| `NOTIFICATION_SERVICE_URL` | URL do notification-service |

### Reporting Service

| Variável | Descrição |
|----------|-----------|
| `CLOUDINARY_CLOUD_NAME` | Cloud name Cloudinary |
| `CLOUDINARY_API_KEY` | API Key Cloudinary |
| `CLOUDINARY_API_SECRET` | API Secret Cloudinary |
| `CLICKSIGN_API_KEY` | API Key Clicksign |
| `CLICKSIGN_BASE_URL` | URL base Clicksign |

### Notification Service

| Variável | Descrição |
|----------|-----------|
| `MAILER_HOST` | SMTP host |
| `MAILER_PORT` | SMTP port (587) |
| `MAILER_FROM` | Email remetente |
| `MAILER_USERNAME` | SMTP / API user |
| `MAILER_PASSWORD` | SMTP / API key |
| `REPORTING_SERVICE_URL` | URL do reporting-service |

### Billing Service

| Variável | Descrição |
|----------|-----------|
| `BILLING_WEBHOOK_TOKEN` | Token para validar webhooks |
| `STRIPE_API_KEY` | Stripe API key (se usado) |
| `STRIPE_WEBHOOK_SECRET` | Stripe webhook secret |

---

## Health Checks e Observabilidade

### Endpoints de Saúde (Quarkus SmallRye Health)

| Endpoint | Tipo | Descrição |
|----------|------|-----------|
| `/q/health/live` | Liveness | Serviço está vivo |
| `/q/health/ready` | Readiness | Dependências (DB) OK |
| `/q/health` | Combined | Liveness + Readiness |
| `/q/metrics` | Prometheus | Métricas HTTP, JVM, DB pool |

### Azure Container Apps — Probes

```
livenessProbe:
  httpGet:
    path: /q/health/live
    port: 8080
  initialDelaySeconds: 10
  periodSeconds: 30

readinessProbe:
  httpGet:
    path: /q/health/ready
    port: 8080
  initialDelaySeconds: 5
  periodSeconds: 15
```

### Logging Estruturado (JSON)

```json
{
  "timestamp": "2026-06-03T14:30:00.000Z",
  "level": "INFO",
  "logger": "com.example.AuthenticationService",
  "message": "User login successful",
  "tenantId": "uuid",
  "userId": "uuid",
  "requestId": "trace-id",
  "durationMs": 145
}
```
