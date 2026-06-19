# Brasaller — Visão Geral da Arquitetura

## Equipe Responsável

| Engenheiro Full Stack | **Vinicius Moreira** |

---

## Resumo Executivo

**Brasaller** é uma plataforma SaaS multi-tenant para vendedores e contadores de e-commerce. Permite conectar marketplaces (Mercado Livre, Shopee, Amazon), consolidar vendas, gerar relatórios financeiros, calcular DRE e gerenciar fechamentos contábeis com assinatura digital.

## Stack Tecnológico

| Camada | Tecnologia |
|--------|-----------|
| Backend | Quarkus 3.35.4 · Java 21 |
| Frontend | Angular 18+ · TypeScript · RxJS |
| Banco de Dados | PostgreSQL (Neon) · Flyway |
| Auth | Keycloak (OAuth2/OIDC) · JWT |
| Infraestrutura | Azure Container Apps · Terraform |
| Email | Quarkus Mailer (Resend/SendGrid) |
| Armazenamento | Cloudinary (anexos) |
| Assinatura Digital | Clicksign |
| Observabilidade | Prometheus · Azure Log Analytics |

---

## Diagrama de Componentes (C4 Level 2)

```mermaid
C4Context
    title Brasaller — Contexto do Sistema

    Person(seller, "Vendedor", "Opera a plataforma via browser")
    Person(accountant, "Contador", "Acessa relatórios do cliente")

    System(brasaller, "Brasaller Platform", "SaaS multi-tenant para gestão de vendas em marketplaces")

    System_Ext(mercadolivre, "Mercado Livre API", "Pedidos, pagamentos, taxas")
    System_Ext(keycloak, "Keycloak", "OAuth2/OIDC Identity Provider")
    System_Ext(clicksign, "Clicksign", "Assinatura digital de fechamentos")
    System_Ext(cloudinary, "Cloudinary", "Upload de anexos de despesas")
    System_Ext(mailer, "Resend/SendGrid", "Envio de emails")

    Rel(seller, brasaller, "Usa", "HTTPS")
    Rel(accountant, brasaller, "Acessa relatórios", "HTTPS")
    Rel(brasaller, mercadolivre, "Sincroniza pedidos", "REST/HTTPS")
    Rel(brasaller, keycloak, "Autentica usuários", "OAuth2")
    Rel(brasaller, clicksign, "Assina fechamentos", "REST/HTTPS")
    Rel(brasaller, cloudinary, "Armazena anexos", "REST/HTTPS")
    Rel(brasaller, mailer, "Envia notificações", "SMTP/REST")
```

---

## Diagrama de Arquitetura de Microserviços

```mermaid
graph TB
    subgraph "Frontend (Angular)"
        UI[fa:fa-browser Braseller App<br/>Angular 18+]
    end

    subgraph "Azure Container Apps"
        GW[Gateway API<br/>:8080]

        subgraph "Core Domain Services"
            AUTH[Auth Service<br/>:8085]
            USER[User Service<br/>:8084]
            BILLING[Billing Service<br/>:8082]
        end

        subgraph "Business Services"
            CORE[Core Service<br/>:8081]
            REPORT[Reporting Service<br/>:8087]
            NOTIF[Notification Service<br/>:8083]
        end
    end

    subgraph "Databases (PostgreSQL - Neon)"
        DB_AUTH[(auth-db)]
        DB_USER[(user-db)]
        DB_BILLING[(billing-db)]
        DB_CORE[(core-db)]
        DB_REPORT[(reporting-db)]
        DB_NOTIF[(notification-db)]
        DB_GW[(gateway-db)]
    end

    subgraph "External Services"
        KC[Keycloak<br/>OAuth2/OIDC]
        ML[Mercado Livre API]
        CS[Clicksign]
        CDN[Cloudinary]
        MAIL[Email Provider]
    end

    UI -->|HTTPS| GW
    GW -->|HTTP| AUTH
    GW -->|HTTP| USER
    GW -->|HTTP| BILLING
    GW -->|HTTP| CORE
    GW -->|HTTP| REPORT
    GW -->|HTTP| NOTIF

    AUTH --- DB_AUTH
    USER --- DB_USER
    BILLING --- DB_BILLING
    CORE --- DB_CORE
    REPORT --- DB_REPORT
    NOTIF --- DB_NOTIF
    GW --- DB_GW

    AUTH -->|Admin API| KC
    USER -->|Admin API| KC
    CORE -->|REST| ML
    CORE -->|Outbox HTTP| NOTIF
    CORE -->|Outbox HTTP| REPORT
    NOTIF -->|Internal API| REPORT
    REPORT -->|REST| CS
    REPORT -->|REST| CDN
    NOTIF -->|SMTP/REST| MAIL
    AUTH -->|Internal HTTP| USER

    style UI fill:#4A90D9,color:#fff
    style GW fill:#E8A838,color:#fff
    style AUTH fill:#5DAA68,color:#fff
    style USER fill:#5DAA68,color:#fff
    style BILLING fill:#5DAA68,color:#fff
    style CORE fill:#9B59B6,color:#fff
    style REPORT fill:#9B59B6,color:#fff
    style NOTIF fill:#9B59B6,color:#fff
```

---

## Padrões Arquiteturais

| Padrão | Aplicação |
|--------|-----------|
| **Microservices** | 7 serviços independentes com banco próprio |
| **API Gateway** | Ponto de entrada único; roteamento dinâmico |
| **Domain-Driven Design** | Cada serviço possui domínio e entidades próprias |
| **Multi-tenancy** | `tenant_id` em todas as tabelas; isolamento total |
| **Outbox Pattern** | Publicação confiável de eventos via tabela + dispatcher |
| **Event-Driven Realtime** | Log durável, replay por cursor, SSE e WebSocket ([detalhes](docs/realtime-connectors.md)) |
| **Connector Pattern** | Integrações de marketplace plugáveis |
| **Clean Architecture** | Separação: interfaces → application → domain → infrastructure |
| **Async Jobs** | Sync de pedidos e DRE via tabelas de job |
| **OAuth2/OIDC** | Keycloak como IdP centralizado |
| **IaC** | Terraform para deploy reproduzível no Azure |

---

## Fluxo de Autenticação

```mermaid
sequenceDiagram
    actor U as Usuário
    participant FE as Frontend (Angular)
    participant GW as Gateway API
    participant AS as Auth Service
    participant US as User Service
    participant KC as Keycloak

    U->>FE: Login (email + senha)
    FE->>GW: POST /api/auth/login
    GW->>AS: POST /auth/login
    AS->>KC: Resource Owner Password Flow
    KC-->>AS: OAuth Token
    AS->>US: POST /users/internal/identity/sync-profile
    US-->>AS: Perfil sincronizado
    AS-->>GW: AuthTokenSet (JWT + RefreshToken)
    GW-->>FE: AuthTokenSet
    FE->>FE: Salva sessão (localStorage)
    FE->>U: Redireciona para /dashboard
```

---

## Fluxo de Sincronização de Pedidos

```mermaid
sequenceDiagram
    actor U as Vendedor
    participant FE as Frontend
    participant CS as Core Service
    participant ML as Mercado Livre API
    participant RS as Reporting Service
    participant NS as Notification Service

    U->>FE: Clica "Sincronizar"
    FE->>CS: POST /core/connectors/mercadolivre/sync-all
    CS->>CS: Cria SyncJob (PENDING)
    CS-->>FE: { jobId }

    loop Polling status
        FE->>CS: GET /core/connectors/sync-jobs/{jobId}
        CS-->>FE: { status: PROCESSING }
    end

    CS->>ML: GET /orders (paginado)
    ML-->>CS: Pedidos normalizados

    CS->>CS: Persiste em connector_sync_jobs
    CS->>CS: Persiste ReportEntry em outbox_events

    Note over CS: Dispatcher periódico (5s)
    CS->>RS: POST /reports/internal/entries
    CS->>NS: POST /notifications/events/new-sale
    RS-->>CS: 200 OK
    NS-->>CS: 200 OK
    CS->>CS: Marca outbox como PROCESSED

    FE->>CS: GET /core/connectors/sync-jobs/{jobId}
    CS-->>FE: { status: COMPLETE }
    FE->>U: Exibe notificação de sucesso
```

---

## Módulos e Responsabilidades

| Serviço | Porta | Responsabilidade Principal |
|---------|-------|---------------------------|
| `gateway-api` | 8080 | Roteamento, CORS, proxy de requests |
| `auth-service` | 8085 | Login, registro, OAuth Google, JWT |
| `user-service` | 8084 | Tenants, usuários, roles, senhas |
| `billing-service` | 8082 | Planos, assinaturas, trial, webhooks |
| `core-service` | 8081 | Conectores de marketplace, pedidos, sync |
| `reporting-service` | 8087 | Dashboard, DRE, despesas, fechamento |
| `notification-service` | 8083 | Notificações in-app, emails, alertas |
| `apps/braseller` | 4200 | Frontend Angular do vendedor/contador |
