# UML dos Microserviços — Diagrama de Classes e Componentes

## 1. Auth Service

```mermaid
classDiagram
    class AuthResource {
        +getStatus() String
        +register(RegisterRequest) AuthTokenSet
        +login(LoginRequest) AuthTokenSet
        +refresh(RefreshRequest) AuthTokenSet
        +logout(RefreshRequest) LogoutResponse
        +getGoogleAuthorizeUrl() GoogleAuthorizeUrlResponse
        +googleCallback(GoogleCallbackRequest) AuthTokenSet
    }

    class AuthenticationService {
        -KeycloakGateway keycloakGateway
        -UserServiceClient userServiceClient
        -JwtIssuer jwtIssuer
        -AuthIdentityRepository identityRepo
        -AuthSessionRepository sessionRepo
        +register(RegisterRequest) AuthTokenSet
        +login(LoginRequest) AuthTokenSet
        +refresh(RefreshRequest) AuthTokenSet
        +logout(RefreshRequest) void
        +googleCallback(GoogleCallbackRequest) AuthTokenSet
        -ensureIdentity(email, tenantId, userId) AuthIdentity
    }

    class AuthIdentity {
        +UUID id
        +UUID tenantId
        +UUID userId
        +String email
        +String fullName
        +String roles
        +String status
        +String provider
        +String providerSubject
        +Instant createdAt
    }

    class AuthTokenSet {
        +String accessToken
        +String refreshToken
        +Instant expiresAt
        +String tenantId
        +String userId
        +String email
        +List~String~ roles
    }

    class AuthSession {
        +UUID id
        +UUID tenantId
        +UUID userId
        +String tokenId
        +String refreshTokenHash
        +Instant expiresAt
        +Instant revokedAt
    }

    AuthResource --> AuthenticationService
    AuthenticationService --> AuthIdentity
    AuthenticationService --> AuthSession
    AuthenticationService --> AuthTokenSet
```

---

## 2. User Service

```mermaid
classDiagram
    class UserResource {
        +getStatus() String
        +registerTenant(RegisterTenantRequest) RegisteredTenant
        +grantAccountantAccess(tenantId, GrantAccountantAccessCommand) AccountantAccessView
        +listMembers(tenantId) List~UserView~
        +verifyPassword(VerifyPasswordRequest) IdentityVerification
        +syncProfile(SyncProfileRequest) void
    }

    class UserIdentityService {
        -JdbcUserIdentityRepository userRepo
        -KeycloakAdminClient keycloakAdmin
        +registerTenant(RegisterTenantRequest) RegisteredTenant
        +grantAccountantAccess(tenantId, cmd) AccountantAccessView
        +listMembers(tenantId) List~UserView~
        +verifyPassword(email, password) IdentityVerification
        +syncExternalProfile(userId, attributes) void
    }

    class Tenant {
        +UUID id
        +String legalName
        +String tradeName
        +String status
        +Instant createdAt
    }

    class UserAccount {
        +UUID id
        +UUID tenantId
        +String email
        +String fullName
        +String passwordHash
        +String provider
        +String status
        +Instant createdAt
    }

    class UserRole {
        +UUID tenantId
        +UUID userId
        +String role
    }

    class AccountantAccess {
        +UUID id
        +UUID tenantId
        +UUID accountantUserId
        +UUID grantedByUserId
        +Boolean readOnly
        +String status
        +Instant createdAt
    }

    UserResource --> UserIdentityService
    UserIdentityService --> Tenant
    UserIdentityService --> UserAccount
    UserIdentityService --> UserRole
    UserIdentityService --> AccountantAccess
    UserAccount "*" --> "1" Tenant
    UserRole "*" --> "1" UserAccount
    AccountantAccess "*" --> "1" Tenant
```

---

## 3. Core Service (Conectores de Marketplace)

```mermaid
classDiagram
    class ConnectorResource {
        +listConnectors() List~ConnectorDescriptor~
        +authenticate(connectorName, AuthenticateRequest) void
        +refreshToken(connectorName) void
        +listOrders(connectorName, filters) List~StandardOrder~
        +getOrder(connectorName, orderId) StandardOrder
        +getPayments(connectorName, orderId) PaymentInfo
        +getFees(connectorName, orderId) FeeInfo
        +listInvoices(connectorName, filters) List~InvoiceInfo~
        +syncAll(connectorName) SyncAccepted
        +getSyncJobStatus(jobId) SyncJob
        +getConnectorStatus(connectorName) ConnectorStatus
    }

    class ConnectorService {
        -ConnectorRegistry registry
        -ConnectorTokenRepository tokenRepo
        -ConnectorSyncJobRepository syncJobRepo
        -OutboxEventRepository outboxRepo
        +authenticate(name, tenantId, code) void
        +getOrders(name, tenantId, filters) List~StandardOrder~
        +syncAll(name, tenantId) SyncAccepted
        -encryptToken(token) String
    }

    class MarketplaceConnector {
        <<interface>>
        +getName() String
        +authenticate(code) ConnectorToken
        +refreshToken(token) ConnectorToken
        +getOrders(token, filters) List~StandardOrder~
        +getOrder(token, orderId) StandardOrder
    }

    class MercadoLibreConnector {
        +getName() String
        +authenticate(code) ConnectorToken
        +getOrders(token, filters) List~StandardOrder~
    }

    class StandardOrder {
        +String orderId
        +String platform
        +LocalDate saleDate
        +BigDecimal grossValue
        +BigDecimal receivedValue
        +BigDecimal feeValue
        +BigDecimal receivableValue
        +String paymentMethod
        +String status
        +LocalDate releaseDate
        +String buyerName
    }

    class ConnectorToken {
        +UUID tenantId
        +String connectorName
        +String sellerId
        +String accessToken
        +String refreshToken
        +Instant expiresAt
    }

    class SyncJob {
        +UUID id
        +UUID tenantId
        +String connectorName
        +String status
        +Instant createdAt
    }

    class OutboxEvent {
        +UUID id
        +String aggregateType
        +String eventType
        +String payload
        +Instant createdAt
        +Instant processedAt
    }

    ConnectorResource --> ConnectorService
    ConnectorService --> MarketplaceConnector
    MarketplaceConnector <|.. MercadoLibreConnector
    ConnectorService --> StandardOrder
    ConnectorService --> ConnectorToken
    ConnectorService --> SyncJob
    ConnectorService --> OutboxEvent
```

---

## 4. Billing Service

```mermaid
classDiagram
    class BillingResource {
        +getStatus() String
        +listPlans() List~BillingPlan~
        +getSubscription(tenantId) BillingSubscription
        +startTrial(tenantId) BillingSubscription
        +changePlan(tenantId, ChangePlanRequest) BillingSubscription
        +receiveWebhook(BillingWebhookEvent) void
    }

    class BillingService {
        -BillingPlanRepository planRepo
        -BillingSubscriptionRepository subRepo
        -BillingWebhookEventRepository webhookRepo
        -BillingProviderGateway providerGateway
        +listPlans() List~BillingPlan~
        +getSubscription(tenantId) BillingSubscription
        +startTrial(tenantId) BillingSubscription
        +changePlan(tenantId, planCode) BillingSubscription
        +applyWebhookEvent(event) void
    }

    class BillingPlan {
        +String code
        +String name
        +BigDecimal monthlyPrice
        +String currency
        +Integer trialDays
        +Integer marketplaceLimit
        +Integer userLimit
        +Boolean active
    }

    class BillingSubscription {
        +UUID id
        +UUID tenantId
        +String planCode
        +SubscriptionStatus status
        +String provider
        +Instant trialStartedAt
        +Instant trialEndsAt
        +Instant suspendedAt
        +String cancellationReason
    }

    class SubscriptionStatus {
        <<enumeration>>
        TRIALING
        ACTIVE
        SUSPENDED
        CANCELLED
    }

    class BillingWebhookEvent {
        +UUID id
        +String provider
        +String providerEventId
        +UUID tenantId
        +String eventType
        +String status
        +Instant receivedAt
        +String payload
    }

    class BillingProviderGateway {
        <<interface>>
        +createCustomer(tenant) String
        +createSubscription(customerId, planCode) String
        +cancelSubscription(subscriptionId) void
    }

    BillingResource --> BillingService
    BillingService --> BillingPlan
    BillingService --> BillingSubscription
    BillingService --> BillingWebhookEvent
    BillingService --> BillingProviderGateway
    BillingSubscription --> SubscriptionStatus
    BillingSubscription "*" --> "1" BillingPlan
```

---

## 5. Notification Service

```mermaid
classDiagram
    class NotificationResource {
        +getPreferences(tenantId) NotificationPreference
        +updatePreferences(tenantId, prefs) void
        +listNotifications(tenantId) List~NotificationMessage~
        +markAsRead(tenantId, notifId) void
        +clearRead(tenantId) void
        +getNewSaleSummary(tenantId) TenantNewSaleSummary
        +onNewSaleEvent(event) void
        +onMlPaymentRelease(event) void
        +onMonthlyClosing(event) void
        +onWeeklyAccountantReport(event) void
    }

    class NotificationService {
        -NotificationPreferenceRepository prefRepo
        -NotificationRepository notifRepo
        -ReportingServiceClient reportingClient
        -MailerService mailerService
        +handleNewSale(event) void
        +handleMlPaymentRelease(event) void
        +triggerMonthlyClosing(tenantId) void
        +triggerWeeklyAccountantReport(tenantId) void
        -sendEmail(to, subject, template, data) void
        -saveInApp(tenantId, type, title, msg) void
    }

    class NotificationPreference {
        +UUID tenantId
        +Boolean emailEnabled
        +Boolean newSaleEnabled
        +Boolean monthlyClosingEnabled
        +Boolean mlPaymentReleaseEnabled
        +Boolean weeklyAccountantReportEnabled
        +String accountantEmail
    }

    class NotificationMessage {
        +UUID id
        +UUID tenantId
        +NotificationType type
        +String title
        +String message
        +String recipientEmail
        +NotificationChannel channel
        +String status
        +Instant readAt
        +Instant createdAt
    }

    class NotificationType {
        <<enumeration>>
        NEW_SALE
        ML_PAYMENT_RELEASE
        MONTHLY_CLOSING
        WEEKLY_REPORT
    }

    class NotificationChannel {
        <<enumeration>>
        IN_APP
        EMAIL
    }

    NotificationResource --> NotificationService
    NotificationService --> NotificationPreference
    NotificationService --> NotificationMessage
    NotificationMessage --> NotificationType
    NotificationMessage --> NotificationChannel
```

---

## 6. Reporting Service

```mermaid
classDiagram
    class ReportingResource {
        +getDashboard(tenantId, filters) DashboardView
        +getSummary(tenantId, filters) FinancialSummary
        +getEntries(tenantId, filters) List~ReportEntry~
        +importEntries(tenantId, entries) void
        +getMonthlyEvolutionChart(tenantId) ChartData
        +getPlatformComparisonChart(tenantId) ChartData
        +getFiscalProfile(tenantId) FiscalProfile
        +saveFiscalProfile(tenantId, profile) void
        +getExpenses(tenantId, filters) List~ExpenseEntry~
        +createExpense(tenantId, expense) ExpenseEntry
        +updateExpense(tenantId, id, expense) ExpenseEntry
        +deleteExpense(tenantId, id) void
        +getDre(tenantId, period) DreStatement
        +queueDreJob(tenantId, period) DreJob
        +getDreJobStatus(tenantId, jobId) DreJob
        +getPeriodClosing(tenantId, month) AccountingPeriodClosing
        +signClosing(tenantId, month) void
        +exportMonthly(tenantId, month, format) Blob
        +ingestEntries(entries) void
        +getSummaryInternal(tenantId, filters) FinancialSummary
        +getPaymentReleases(tenantId) List~PaymentReleaseAlert~
    }

    class ReportingService {
        -ReportEntryRepository entryRepo
        -ExpenseRepository expenseRepo
        -FiscalProfileRepository fiscalRepo
        -AccountingPeriodRepository closingRepo
        -DreJobRepository dreRepo
        -CloudinaryClient cloudinary
        -ClicksignClient clicksign
        +getDashboard(tenantId, filters) DashboardView
        +getFinancialSummary(tenantId, filters) FinancialSummary
        +calculateDre(tenantId, from, to) DreStatement
        +signPeriodClosing(tenantId, month, userId) void
        +exportReport(tenantId, period, format) byte[]
    }

    class ReportEntry {
        +UUID id
        +UUID tenantId
        +String platform
        +String orderId
        +LocalDate saleDate
        +BigDecimal grossValue
        +BigDecimal receivedValue
        +BigDecimal feeValue
        +BigDecimal receivableValue
        +String paymentMethod
        +String status
        +LocalDate releaseDate
        +String buyerName
        +String invoiceNumber
    }

    class FinancialSummary {
        +BigDecimal grossRevenue
        +BigDecimal received
        +BigDecimal fees
        +BigDecimal receivable
        +Integer totalOrders
    }

    class FiscalProfile {
        +UUID tenantId
        +TaxRegime taxRegime
        +BigDecimal estimatedTaxRate
        +String notes
    }

    class TaxRegime {
        <<enumeration>>
        SIMPLES_NACIONAL
        LUCRO_PRESUMIDO
        LUCRO_REAL
        MEI
    }

    class ExpenseEntry {
        +UUID id
        +UUID tenantId
        +LocalDate expenseDate
        +String category
        +String description
        +BigDecimal amount
        +String attachmentUrl
    }

    class DreStatement {
        +BigDecimal grossRevenue
        +BigDecimal fees
        +BigDecimal taxes
        +BigDecimal expenses
        +BigDecimal netProfit
        +BigDecimal profitMargin
    }

    class AccountingPeriodClosing {
        +UUID tenantId
        +String month
        +String signatureHash
        +UUID signedByUserId
        +Instant signedAt
        +String dreStatementJson
    }

    ReportingResource --> ReportingService
    ReportingService --> ReportEntry
    ReportingService --> FinancialSummary
    ReportingService --> FiscalProfile
    ReportingService --> ExpenseEntry
    ReportingService --> DreStatement
    ReportingService --> AccountingPeriodClosing
    FiscalProfile --> TaxRegime
```

---

## 7. Gateway API

```mermaid
classDiagram
    class GatewayResource {
        +routeGet(service, path, headers) Response
        +routePost(service, path, headers, body) Response
        +routePut(service, path, headers, body) Response
        +routePatch(service, path, headers, body) Response
        +routeDelete(service, path, headers) Response
    }

    class GatewayRoutingService {
        -Map~String, String~ serviceUrlMap
        -RestClient restClient
        +route(method, service, path, headers, body) Response
        -buildDownstreamUrl(service, path) String
        -propagateHeaders(headers) Map
    }

    class ServiceMetadata {
        +UUID id
        +String serviceName
        +String baseUrl
        +String ingressExternal
        +Instant createdAt
    }

    class ServiceRouteMap {
        <<static>>
        auth → http://auth-service:8085
        user → http://user-service:8084
        billing → http://billing-service:8082
        core → http://core-service:8081
        reporting → http://reporting-service:8087
        notification → http://notification-service:8083
    }

    GatewayResource --> GatewayRoutingService
    GatewayRoutingService --> ServiceMetadata
    GatewayRoutingService --> ServiceRouteMap
```

---

## 8. Frontend Angular — Estrutura de Módulos

```mermaid
classDiagram
    class AppRoutes {
        / → redirect /login
        /login → LoginPage [noAuthGuard]
        /auth/callback → LoginPage
        /dashboard → DashboardShell [authGuard]
        /unauthorized → UnauthorizedPage
        /** → NotFoundPage
    }

    class DashboardShell {
        +tabs: Dashboard | Lançamentos | Despesas | DRE
        +sidebar: Connectors | Profile | Settings
    }

    class AuthService {
        -localStorage: AuthSession
        +login(email, password) Observable~AuthSession~
        +register(name, email, password) Observable~AuthSession~
        +startGoogleLogin() void
        +completeGoogleCallback(code) Observable~AuthSession~
        +refresh() Observable~AuthSession~
        +logout() void
        +isAuthenticated() boolean
        +getSession() AuthSession
    }

    class ApiService {
        -baseUrl: string
        -httpClient: HttpClient
        +get~T~(path, params) Observable~T~
        +post~T~(path, body) Observable~T~
        +put~T~(path, body) Observable~T~
        +patch~T~(path, body) Observable~T~
        +delete~T~(path) Observable~T~
        +getBlob(path) Observable~Blob~
    }

    class CoreService {
        +listConnectors() Observable~Connector[]~
        +authenticateConnector(name, code) Observable~void~
        +getOrders(name, filters) Observable~StandardOrder[]~
        +syncAll(name) Observable~SyncAccepted~
        +getSyncStatus(jobId) Observable~SyncJob~
    }

    class ReportingService {
        +getDashboard(filters) Observable~DashboardView~
        +getSummary(filters) Observable~FinancialSummary~
        +getEntries(filters) Observable~ReportEntry[]~
        +getExpenses(filters) Observable~ExpenseEntry[]~
        +getDre(period) Observable~DreStatement~
        +exportMonthly(month, format) Observable~Blob~
    }

    class NotificationService {
        +getPreferences() Observable~NotificationPreference~
        +updatePreferences(prefs) Observable~void~
        +list() Observable~Notification[]~
    }

    class AuthGuard {
        +canActivate(route) boolean
        -redirectToLogin() void
    }

    class NoAuthGuard {
        +canActivate(route) boolean
        -redirectToDashboard() void
    }

    AppRoutes --> DashboardShell
    DashboardShell --> CoreService
    DashboardShell --> ReportingService
    DashboardShell --> NotificationService
    AuthService --> ApiService
    CoreService --> ApiService
    ReportingService --> ApiService
    NotificationService --> ApiService
    AuthGuard --> AuthService
    NoAuthGuard --> AuthService
```

---

## Diagrama de Deployment (Azure)

```mermaid
graph TB
    subgraph RG["Azure Resource Group"]
        subgraph CAE["Container App Environment"]
            subgraph CA["Container Apps"]
                GW_CA["gateway-api"]
                AUTH_CA["auth-service"]
                USER_CA["user-service"]
                BILLING_CA["billing-service"]
                CORE_CA["core-service"]
                REPORT_CA["reporting-service"]
                NOTIF_CA["notification-service"]
            end
            LA["Log Analytics Workspace"]
        end
        ACR["Azure Container Registry"]
        MI["Managed Identity - RBAC para ACR"]
    end

    subgraph EXT["External Services"]
        NEON["Neon PostgreSQL - 1 DB por service"]
        KC_EXT["Keycloak - OAuth2/OIDC"]
    end

    CI["GitHub Actions CI/CD"] -->|docker push| ACR
    ACR --> GW_CA
    ACR --> AUTH_CA
    ACR --> USER_CA
    ACR --> BILLING_CA
    ACR --> CORE_CA
    ACR --> REPORT_CA
    ACR --> NOTIF_CA

    GW_CA --> AUTH_CA
    GW_CA --> USER_CA
    GW_CA --> BILLING_CA
    GW_CA --> CORE_CA
    GW_CA --> REPORT_CA
    GW_CA --> NOTIF_CA

    AUTH_CA --> NEON
    USER_CA --> NEON
    BILLING_CA --> NEON
    CORE_CA --> NEON
    REPORT_CA --> NEON
    NOTIF_CA --> NEON

    AUTH_CA --> KC_EXT
    USER_CA --> KC_EXT

    GW_CA -.->|ingress public HTTPS| Internet(["Internet"])

    style GW_CA fill:#E8A838,color:#000
    style ACR fill:#0078D4,color:#fff
    style NEON fill:#336791,color:#fff
```
