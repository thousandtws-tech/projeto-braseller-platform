# Diagrama de Entidades e Relacionamentos (ERD)

Cada microserviço possui seu próprio banco PostgreSQL (database-per-service). Os relacionamentos entre serviços são gerenciados pela aplicação, não por foreign keys cross-database.

---

## Auth Service Database

```mermaid
erDiagram
    auth_identities {
        uuid id PK
        uuid tenant_id
        uuid user_id
        varchar email UK
        varchar full_name
        varchar password_hash
        varchar roles
        varchar status
        varchar provider
        varchar provider_subject
        timestamp created_at
        timestamp updated_at
    }

    auth_sessions {
        uuid id PK
        uuid tenant_id
        uuid user_id
        varchar token_id UK
        varchar refresh_token_hash UK
        timestamp expires_at
        timestamp revoked_at
        timestamp created_at
    }

    auth_identities ||--o{ auth_sessions : "user_id → sessions"
```

---

## User Service Database

```mermaid
erDiagram
    tenants {
        uuid id PK
        varchar legal_name
        varchar trade_name
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    user_accounts {
        uuid id PK
        uuid tenant_id FK
        varchar email UK
        varchar full_name
        varchar password_hash
        varchar provider
        varchar provider_subject
        varchar status
        timestamp created_at
        timestamp updated_at
    }

    user_roles {
        uuid tenant_id FK
        uuid user_id FK
        varchar role
        timestamp created_at
    }

    accountant_access {
        uuid id PK
        uuid tenant_id FK
        uuid accountant_user_id FK
        uuid granted_by_user_id FK
        boolean read_only
        varchar status
        timestamp created_at
        timestamp revoked_at
    }

    tenants ||--o{ user_accounts : "tenant_id"
    user_accounts ||--o{ user_roles : "user_id"
    tenants ||--o{ user_roles : "tenant_id"
    tenants ||--o{ accountant_access : "tenant_id"
    user_accounts ||--o{ accountant_access : "accountant_user_id"
```

---

## Core Service Database

```mermaid
erDiagram
    marketplace_connector_tokens {
        uuid tenant_id PK
        varchar connector_name PK
        varchar seller_id
        text access_token
        text refresh_token
        timestamp expires_at
        timestamp updated_at
    }

    connector_sync_jobs {
        uuid id PK
        uuid tenant_id
        varchar connector_name
        varchar status
        date since
        integer limit_val
        json result
        timestamp created_at
        timestamp updated_at
    }

    outbox_events {
        uuid id PK
        varchar aggregate_type
        varchar aggregate_id
        varchar event_type
        json payload
        timestamp created_at
        timestamp processed_at
    }

    marketplace_connector_tokens ||--o{ connector_sync_jobs : "tenant_id + connector_name"
```

---

## Billing Service Database

```mermaid
erDiagram
    billing_plans {
        varchar code PK
        varchar name
        varchar description
        decimal monthly_price
        varchar currency
        integer trial_days
        integer marketplace_limit
        integer user_limit
        boolean active
    }

    billing_subscriptions {
        uuid id PK
        uuid tenant_id UK
        varchar plan_code FK
        varchar status
        varchar provider
        varchar provider_customer_id
        varchar provider_subscription_id
        timestamp trial_started_at
        timestamp trial_ends_at
        timestamp current_period_start
        timestamp current_period_end
        timestamp suspended_at
        varchar cancellation_reason
        varchar latest_event_id
        timestamp created_at
        timestamp updated_at
    }

    billing_webhook_events {
        uuid id PK
        varchar provider
        varchar provider_event_id UK
        uuid tenant_id
        varchar event_type
        varchar status
        timestamp received_at
        text payload
    }

    billing_plans ||--o{ billing_subscriptions : "plan_code"
```

---

## Notification Service Database

```mermaid
erDiagram
    notification_preferences {
        uuid tenant_id PK
        boolean email_enabled
        boolean new_sale_enabled
        boolean monthly_closing_enabled
        boolean ml_payment_release_enabled
        boolean weekly_accountant_report_enabled
        varchar accountant_email
        timestamp updated_at
    }

    notifications {
        uuid id PK
        uuid tenant_id
        varchar type
        varchar title
        text message
        varchar recipient_email
        varchar channel
        varchar status
        timestamp read_at
        timestamp created_at
    }

    notification_deliveries {
        uuid id PK
        uuid notification_id FK
        varchar channel
        varchar status
        text error_message
        timestamp created_at
    }

    new_sale_summary {
        uuid tenant_id PK
        date summary_date PK
        integer total_sales
        decimal total_amount
        timestamp created_at
    }

    notification_preferences ||--o{ notifications : "tenant_id"
    notifications ||--o{ notification_deliveries : "notification_id"
    notification_preferences ||--o{ new_sale_summary : "tenant_id"
```

---

## Reporting Service Database

```mermaid
erDiagram
    report_entries {
        uuid id PK
        uuid tenant_id
        varchar platform
        varchar order_id
        date sale_date
        decimal gross_value
        decimal received_value
        decimal fee_value
        decimal receivable_value
        varchar payment_method
        varchar status
        date release_date
        varchar buyer_name
        varchar invoice_number
        timestamp created_at
        timestamp updated_at
    }

    fiscal_profile {
        uuid tenant_id PK
        varchar tax_regime
        decimal estimated_tax_rate
        text notes
        timestamp updated_at
    }

    expenses {
        uuid id PK
        uuid tenant_id
        date expense_date
        varchar category
        varchar description
        decimal amount
        text attachment_url
        json attachment_metadata
        timestamp created_at
        timestamp updated_at
    }

    accounting_period_closings {
        uuid tenant_id PK
        varchar month PK
        varchar signature_hash
        uuid signed_by_user_id
        timestamp signed_at
        json dre_statement
        timestamp created_at
    }

    dre_jobs {
        uuid id PK
        uuid tenant_id
        date from_date
        date to_date
        varchar status
        json result
        timestamp created_at
        timestamp updated_at
    }

    outbox_events {
        uuid id PK
        varchar aggregate_type
        varchar aggregate_id
        varchar event_type
        json payload
        timestamp created_at
        timestamp processed_at
    }

    report_entries }o--|| fiscal_profile : "tenant_id"
    fiscal_profile ||--o{ accounting_period_closings : "tenant_id"
    expenses }o--|| fiscal_profile : "tenant_id"
    fiscal_profile ||--o{ dre_jobs : "tenant_id"
```

---

## Gateway API Database

```mermaid
erDiagram
    service_metadata {
        uuid id PK
        varchar service_name UK
        varchar base_url
        varchar ingress_external
        timestamp created_at
    }
```

---

## Visão Consolidada dos Domínios (Cross-Service)

```mermaid
graph LR
    subgraph "auth-service"
        AI[auth_identities]
        AS2[auth_sessions]
    end

    subgraph "user-service"
        T[tenants]
        UA[user_accounts]
        UR[user_roles]
        AA[accountant_access]
    end

    subgraph "billing-service"
        BP[billing_plans]
        BS[billing_subscriptions]
        BWE[billing_webhook_events]
    end

    subgraph "core-service"
        MCT[marketplace_connector_tokens]
        CSJ[connector_sync_jobs]
        OE1[outbox_events]
    end

    subgraph "reporting-service"
        RE[report_entries]
        FP[fiscal_profile]
        EX[expenses]
        APC[accounting_period_closings]
        DJ[dre_jobs]
        OE2[outbox_events]
    end

    subgraph "notification-service"
        NP[notification_preferences]
        NM[notifications]
        ND[notification_deliveries]
        NSS[new_sale_summary]
    end

    T -.->|tenant_id ref| AI
    T -.->|tenant_id ref| MCT
    T -.->|tenant_id ref| BS
    T -.->|tenant_id ref| RE
    T -.->|tenant_id ref| NP
    OE1 -.->|HTTP dispatch| NM
    OE1 -.->|HTTP dispatch| RE
    OE2 -.->|HTTP dispatch| DJ

    style T fill:#5DAA68,color:#fff
    style AI fill:#E8A838,color:#000
    style MCT fill:#9B59B6,color:#fff
    style BS fill:#3498DB,color:#fff
    style RE fill:#E74C3C,color:#fff
    style NM fill:#1ABC9C,color:#fff
```

---

## Tabelas por Serviço — Resumo

| Serviço | Tabelas | Observações |
|---------|---------|-------------|
| auth-service | auth_identities, auth_sessions | JWT, OAuth, Keycloak sync |
| user-service | tenants, user_accounts, user_roles, accountant_access | Multi-tenant identity |
| billing-service | billing_plans, billing_subscriptions, billing_webhook_events | Assinaturas, trial 14 dias |
| core-service | marketplace_connector_tokens, connector_sync_jobs, outbox_events | Tokens AES-256, jobs async |
| reporting-service | report_entries, fiscal_profile, expenses, accounting_period_closings, dre_jobs, outbox_events | Núcleo financeiro |
| notification-service | notification_preferences, notifications, notification_deliveries, new_sale_summary | Email + in-app |
| gateway-api | service_metadata | Mapa de roteamento |
