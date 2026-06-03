# Diagramas de Sequência — Fluxos Principais

## 1. Registro de Novo Tenant (Onboarding)

```mermaid
sequenceDiagram
    actor U as Usuário
    participant FE as Frontend
    participant GW as Gateway API
    participant AS as Auth Service
    participant US as User Service
    participant KC as Keycloak

    U->>FE: Preenche formulário de registro
    FE->>GW: POST /api/auth/register
    GW->>AS: POST /auth/register

    AS->>US: POST /users/tenants/register
    US->>US: Cria tenant (status: ACTIVE)
    US->>US: Cria user_account + PBKDF2 hash
    US->>US: Cria user_role (ADMIN)
    US->>KC: Cria usuário no Keycloak (Admin API)
    KC-->>US: userId Keycloak
    US-->>AS: RegisteredTenant { tenantId, userId }

    AS->>KC: Autentica (Resource Owner Password)
    KC-->>AS: OAuth token
    AS->>AS: Persiste auth_identity
    AS->>AS: Gera JWT (15 min) + RefreshToken
    AS->>AS: Persiste auth_session

    AS-->>GW: AuthTokenSet
    GW-->>FE: AuthTokenSet
    FE->>FE: Salva sessão (localStorage)
    FE->>U: Redireciona /dashboard
```

---

## 2. Login com Email e Senha

```mermaid
sequenceDiagram
    actor U as Usuário
    participant FE as Frontend
    participant GW as Gateway
    participant AS as Auth Service
    participant US as User Service
    participant KC as Keycloak

    U->>FE: Informa email + senha
    FE->>GW: POST /api/auth/login { email, password }
    GW->>AS: POST /auth/login

    AS->>KC: Resource Owner Password Flow
    alt Credenciais inválidas
        KC-->>AS: 401 Unauthorized
        AS-->>GW: 401 { error: "invalid_credentials" }
        GW-->>FE: 401
        FE->>U: Exibe erro de login
    else Credenciais válidas
        KC-->>AS: { accessToken, refreshToken }
        AS->>US: POST /users/internal/identity/sync-profile
        US-->>AS: Perfil atualizado
        AS->>AS: Upsert auth_identity
        AS->>AS: Gera JWT com claims (tenantId, userId, roles)
        AS->>AS: Persiste auth_session
        AS-->>GW: AuthTokenSet
        GW-->>FE: AuthTokenSet
        FE->>FE: Armazena em localStorage
        FE->>U: Redireciona /dashboard
    end
```

---

## 3. Login com Google OAuth

```mermaid
sequenceDiagram
    actor U as Usuário
    participant FE as Frontend
    participant GW as Gateway
    participant AS as Auth Service
    participant KC as Keycloak
    participant US as User Service

    U->>FE: Clica "Entrar com Google"
    FE->>GW: GET /api/auth/oauth/google/authorize-url
    GW->>AS: GET /auth/oauth/google/authorize-url
    AS-->>GW: { authorizeUrl: "https://keycloak/.../google/authorize..." }
    GW-->>FE: authorizeUrl
    FE->>FE: window.location = authorizeUrl

    Note over U, KC: Usuário autentica no Google via Keycloak
    KC->>FE: Redireciona /auth/callback?code=XXX

    FE->>GW: POST /api/auth/oauth/google/callback { code }
    GW->>AS: POST /auth/oauth/google/callback

    AS->>KC: Troca code por token
    KC-->>AS: { access_token, id_token, profile }

    AS->>AS: Extrai email do id_token
    AS->>AS: Busca auth_identity por email

    alt Novo usuário
        AS->>US: POST /users/tenants/register (auto-provision)
        US-->>AS: RegisteredTenant
        AS->>AS: Cria auth_identity (provider: GOOGLE)
    else Usuário existente
        AS->>US: POST /users/internal/identity/sync-profile
        US-->>AS: OK
    end

    AS->>AS: Gera JWT + RefreshToken
    AS-->>GW: AuthTokenSet
    GW-->>FE: AuthTokenSet
    FE->>U: Redireciona /dashboard
```

---

## 4. Refresh de Token

```mermaid
sequenceDiagram
    participant FE as Frontend (HttpInterceptor)
    participant GW as Gateway
    participant AS as Auth Service
    participant KC as Keycloak

    Note over FE: Requisição falha com 401
    FE->>FE: Detecta token expirado
    FE->>GW: POST /api/auth/refresh { refreshToken }
    GW->>AS: POST /auth/refresh

    AS->>KC: Refresh Grant (refreshToken)
    alt RefreshToken expirado/revogado
        KC-->>AS: 401
        AS-->>GW: 401
        GW-->>FE: 401
        FE->>FE: Limpa sessão
        FE->>FE: Redireciona /login
    else RefreshToken válido
        KC-->>AS: Novo access_token + refresh_token
        AS->>AS: Revoga auth_session antiga
        AS->>AS: Gera novo JWT + nova auth_session
        AS-->>GW: AuthTokenSet
        GW-->>FE: AuthTokenSet
        FE->>FE: Atualiza localStorage
        FE->>FE: Repete requisição original
    end
```

---

## 5. Sincronização de Pedidos (Mercado Livre)

```mermaid
sequenceDiagram
    actor V as Vendedor
    participant FE as Frontend
    participant GW as Gateway
    participant CS as Core Service
    participant ML as Mercado Livre API
    participant RS as Reporting Service
    participant NS as Notification Service

    V->>FE: Clica "Sincronizar Pedidos"
    FE->>GW: POST /api/core/connectors/mercadolivre/sync-all
    GW->>CS: POST /core/connectors/mercadolivre/sync-all

    CS->>CS: Cria SyncJob { status: PENDING }
    CS-->>GW: SyncAccepted { jobId }
    GW-->>FE: { jobId }

    loop Polling a cada 3s
        FE->>GW: GET /api/core/connectors/sync-jobs/{jobId}
        GW->>CS: GET /core/connectors/sync-jobs/{jobId}
        CS-->>GW: { status: PROCESSING }
        GW-->>FE: { status: PROCESSING }
    end

    CS->>CS: Atualiza SyncJob { status: PROCESSING }
    CS->>ML: GET /orders?from=...&to=... (paginado)
    ML-->>CS: Lista de pedidos

    loop Para cada pedido
        CS->>CS: Normaliza para StandardOrder
        CS->>CS: Persiste em outbox_events (ReportEntryUpsertRequestedEvent)
        CS->>CS: Persiste em outbox_events (NewSaleEvent)
    end

    CS->>CS: Atualiza SyncJob { status: COMPLETE }

    Note over CS: Dispatcher periódico (a cada 5s)
    CS->>RS: POST /reports/internal/entries [lote]
    RS->>RS: Upsert report_entries
    RS-->>CS: 200 OK
    CS->>CS: Marca outbox PROCESSED

    CS->>NS: POST /notifications/events/new-sale
    NS->>NS: Verifica notification_preferences
    NS->>NS: Persiste notification (IN_APP)
    opt Email habilitado
        NS->>NS: Envia email "Nova venda"
    end
    NS-->>CS: 200 OK

    FE->>GW: GET /api/core/connectors/sync-jobs/{jobId}
    GW->>CS: GET (status)
    CS-->>GW: { status: COMPLETE }
    GW-->>FE: { status: COMPLETE }
    FE->>V: Exibe toast "Sincronização concluída"
```

---

## 6. Geração de DRE (Demonstração do Resultado)

```mermaid
sequenceDiagram
    actor C as Contador/Vendedor
    participant FE as Frontend
    participant GW as Gateway
    participant RS as Reporting Service

    C->>FE: Seleciona período e clica "Gerar DRE"
    FE->>GW: POST /api/reports/tenants/{id}/dre/jobs { from, to }
    GW->>RS: POST /reports/tenants/{id}/dre/jobs

    RS->>RS: Cria DreJob { status: PENDING }
    RS->>RS: Persiste outbox_events (DreCalculationRequestedEvent)
    RS-->>GW: DreJob { jobId, status: PENDING }
    GW-->>FE: { jobId }

    loop Polling
        FE->>GW: GET /api/reports/tenants/{id}/dre/jobs/{jobId}
        RS-->>FE: { status: PROCESSING }
    end

    Note over RS: Dispatcher / Scheduler processa job
    RS->>RS: Busca report_entries (gross, fees, received)
    RS->>RS: Busca expenses no período
    RS->>RS: Busca fiscal_profile (tax_regime, rate)
    RS->>RS: Calcula DRE:
    Note right of RS: grossRevenue = Σ gross_value\nfees = Σ fee_value\ntaxes = grossRevenue × rate\nexpenses = Σ amount\nnetProfit = grossRevenue - fees - taxes - expenses

    RS->>RS: Atualiza DreJob { status: COMPLETE, result: DreStatement }

    FE->>GW: GET /api/reports/tenants/{id}/dre/jobs/{jobId}
    RS-->>FE: { status: COMPLETE, result: {...} }
    FE->>C: Exibe DRE com receita, despesas, lucro líquido
```

---

## 7. Fechamento Contábil com Assinatura Digital

```mermaid
sequenceDiagram
    actor CT as Contador
    participant FE as Frontend
    participant GW as Gateway
    participant RS as Reporting Service
    participant CS2 as Clicksign

    CT->>FE: Acessa fechamento do mês
    FE->>GW: GET /api/reports/tenants/{id}/closings/202506
    GW->>RS: GET /reports/tenants/{id}/closings/202506

    RS->>RS: Busca accounting_period_closings
    alt Já fechado
        RS-->>FE: { signedAt, signatureHash, dreStatement }
        FE->>CT: Exibe período fechado (read-only)
    else Aberto
        RS->>RS: Calcula DRE do período
        RS-->>FE: DRE + status: OPEN
        CT->>FE: Revisa e clica "Assinar Fechamento"
        FE->>GW: POST /api/reports/tenants/{id}/closings/202506/sign
        GW->>RS: POST /reports/tenants/{id}/closings/202506/sign

        RS->>CS2: POST /api/v1/documents { dreStatement PDF }
        CS2-->>RS: { documentId, signatureUrl }
        RS->>RS: Persiste accounting_period_closings { signatureHash, signedByUserId, signedAt }
        RS-->>FE: { signedAt, documentUrl }
        FE->>CT: Confirma fechamento assinado
    end
```

---

## 8. Concessão de Acesso ao Contador

```mermaid
sequenceDiagram
    actor V as Vendedor (ADMIN)
    participant FE as Frontend
    participant GW as Gateway
    participant US as User Service
    participant KC as Keycloak
    participant NS as Notification Service

    V->>FE: Cadastra email do contador
    FE->>GW: POST /api/users/tenants/{id}/accountants { email, readOnly }
    GW->>US: POST /users/tenants/{id}/accountants

    US->>US: Cria user_account { role: CONTADOR }
    US->>US: Cria user_role { role: CONTADOR }
    US->>US: Cria accountant_access { readOnly: true }
    US->>KC: Cria/vincula usuário no Keycloak
    KC-->>US: OK

    US->>NS: (via evento ou direto) Notifica contador
    NS->>NS: Envia email de boas-vindas ao contador

    US-->>GW: AccountantAccessView
    GW-->>FE: AccountantAccessView
    FE->>V: Confirma acesso concedido
```

---

## 9. Alerta de Liberação de Pagamento ML

```mermaid
sequenceDiagram
    participant SCH as Scheduler (diário)
    participant NS as Notification Service
    participant RS as Reporting Service
    participant MAIL as Email Provider

    SCH->>NS: Trigger check pagamentos a liberar
    NS->>RS: GET /reports/internal/tenants/{id}/payment-releases
    RS->>RS: Busca report_entries WHERE release_date = TOMORROW
    RS-->>NS: List~PaymentReleaseAlert~

    loop Para cada alerta
        NS->>NS: Verifica notification_preferences { ml_payment_release_enabled }
        alt Notificação habilitada
            NS->>NS: Persiste notification (IN_APP)
            NS->>MAIL: Envia email "Pagamento amanhã: R$ X"
            MAIL-->>NS: 200 OK
            NS->>NS: Persiste notification_delivery { status: SENT }
        end
    end
```

---

## 10. Exportação de Relatório (PDF/XLSX/CSV)

```mermaid
sequenceDiagram
    actor U as Usuário
    participant FE as Frontend
    participant GW as Gateway
    participant RS as Reporting Service

    U->>FE: Seleciona mês e formato (PDF/XLSX/CSV)
    FE->>GW: GET /api/reports/tenants/{id}/exports/monthly?month=202506&format=pdf
    GW->>RS: GET /reports/tenants/{id}/exports/monthly

    RS->>RS: Busca report_entries do mês
    RS->>RS: Busca expenses do mês
    RS->>RS: Busca fiscal_profile
    RS->>RS: Calcula FinancialSummary

    alt format=PDF
        RS->>RS: Renderiza PDF (iText)
    else format=XLSX
        RS->>RS: Renderiza XLSX (Apache POI)
    else format=CSV
        RS->>RS: Renderiza CSV
    end

    RS-->>GW: Binary file (Content-Type: application/pdf)
    GW-->>FE: Blob
    FE->>FE: URL.createObjectURL + download
    FE->>U: Browser baixa o arquivo
```
