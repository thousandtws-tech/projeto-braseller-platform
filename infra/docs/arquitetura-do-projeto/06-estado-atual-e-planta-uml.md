# Brasaller - Estado Atual Implementado e Planta UML

Atualizado em: 2026-06-08

Este documento consolida o que ja foi implementado no Brasaller e apresenta uma planta UML profissional do estado atual da plataforma. Ele deve ser usado como referencia para demonstracao ao cliente, alinhamento tecnico e proximas entregas.

## 1. Resumo Executivo

O Brasaller esta implementado como uma plataforma SaaS multi-tenant para lojistas de e-commerce e contadores. O produto ja entrega o nucleo contabil prometido: captura vendas, normaliza taxas/frete, apura despesas, calcula CMV, estima impostos por regime tributario, monta a DRE e libera lucro distribuivel apos fechamento/assinatura.

O diferencial demonstravel hoje e o funil contabil completo:

1. Marketplace envia venda e split financeiro.
2. Core padroniza receita, taxas e frete.
3. Reporting registra lancamentos, despesas, estoque, CMV e banco.
4. DRE calcula resultado liquido e lucro distribuivel.
5. Contador visualiza clientes no BPO e fecha periodos em lote.
6. Lojista visualiza lucro disponivel para distribuicao.

## 2. Estado Atual por Area

| Area | Status | Evidencia funcional |
|------|--------|---------------------|
| Marketplaces / vendas | Feito | Core sincroniza vendas e envia lancamentos normalizados ao Reporting. |
| Split de taxas e frete | Feito | Taxas e frete sao padronizados como custo de marketplace. Amazon, Mercado Livre e Shopee possuem normalizacao dedicada. |
| Despesas manuais | Feito | CRUD de despesas com comprovante obrigatorio. |
| Comprovantes | Feito | Upload de comprovantes via Cloudinary integrado as despesas. |
| OFX bancario | Feito/parcial | Importa OFX/QFX, categoriza debitos e soma despesas bancarias na DRE. Open Finance real ainda nao esta integrado. |
| Estoque / XML NF-e fornecedor | Feito | Upload de XML de fornecedor alimenta estoque, custo unitario e entradas de compra. |
| CMV por venda/SKU | Feito | Venda com item/SKU gera movimento `EXIT` e entra na linha de CMV da DRE. |
| Estorno CMV/estoque | Feito | `CANCELLED` e `REFUNDED` geram reversao `SALE_REVERSAL`, devolvendo estoque e abatendo CMV. |
| Estorno de receita cancelada | Feito | Canceladas/reembolsadas zeram receita, recebido, taxa e a receber nos agregados. |
| DRE | Feito | Calcula receita, taxas/frete, impostos, CMV, despesas, banco, resultado liquido e lucro distribuivel. |
| Regime tributario automatico | Feito | Simples Nacional, Lucro Presumido e Lucro Real calculam impostos automaticamente com aliquota efetiva. |
| Lucro disponivel | Feito | Fechamento assinado libera lucro; distribuicoes registradas consomem saldo disponivel. |
| Assinatura / fechamento | Feito/parcial | Fechamento contabil imutabiliza o periodo; Clicksign/webhook existem. ICP-Brasil A1/A3 nativo ainda nao. |
| Painel contador SaaS | Feito | Contador visualiza multiplos clientes vinculados. |
| BPO multi-cliente | Feito | Painel BPO lista clientes, DRE, status, assinatura em lote e carteira global para operador BPO interno (`BPO_ADMIN` ou `ADMIN` + `CONTADOR`). |
| Modo somente leitura do contador | Feito | Telas operacionais ficam visiveis, mas acoes de escrita aparecem bloqueadas com cadeado animado. |
| CNPJ / Receita Federal | Feito/parcial | Backend consulta BrasilAPI por CNPJ; tela de configuracoes consulta dados cadastrais. |
| API Nota Fiscal / SEFAZ | Falta | Ainda nao emite NF-e nem calcula imposto real por nota emitida. |
| Balanco Patrimonial | Falta | Ainda nao ha Ativo, Passivo e Patrimonio Liquido. |
| Webhooks marketplace | Falta/parcial | Cancelamentos e devolucoes entram por sync/conector; webhook real por marketplace ainda falta. |

## 3. Planta UML - Componentes

```mermaid
flowchart TB
    subgraph Browser["Browser"]
        Client["apps/client<br/>Next.js 16 + Server Actions"]
    end

    subgraph Azure["Azure Container Apps"]
        Gateway["gateway-api<br/>Proxy / API facade"]
        Auth["auth-service<br/>Login, registro, JWT"]
        User["user-service<br/>Tenants, usuarios, contador, CNPJ"]
        Core["core-service<br/>Marketplaces, pedidos, taxas, frete"]
        Reporting["reporting-service<br/>DRE, despesas, estoque, CMV, BPO"]
        Notification["notification-service<br/>Emails e notificacoes"]
        Billing["billing-service<br/>Planos e assinatura SaaS"]
    end

    subgraph External["Servicos externos"]
        Keycloak["Keycloak<br/>OIDC / usuarios externos"]
        Marketplaces["Mercado Livre / Shopee / Amazon"]
        BrasilApi["BrasilAPI CNPJ"]
        Cloudinary["Cloudinary<br/>comprovantes"]
        Clicksign["Clicksign<br/>assinatura"]
        Neon["Neon PostgreSQL<br/>banco por servico"]
    end

    Client -->|HTTPS /api/*| Gateway
    Gateway --> Auth
    Gateway --> User
    Gateway --> Core
    Gateway --> Reporting
    Gateway --> Notification
    Gateway --> Billing

    Auth --> Keycloak
    Auth --> User
    User --> Keycloak
    User --> BrasilApi
    Core --> Marketplaces
    Core -->|outbox HTTP| Reporting
    Core -->|outbox HTTP| Notification
    Reporting --> Cloudinary
    Reporting --> Clicksign

    Auth --- Neon
    User --- Neon
    Core --- Neon
    Reporting --- Neon
    Notification --- Neon
    Billing --- Neon
```

## 4. Planta UML - Dominio Contabil e BPO

```mermaid
classDiagram
    direction LR

    class Tenant {
        +String id
        +String legalName
        +String tradeName
        +String cnpj
        +String status
    }

    class UserAccount {
        +String id
        +String tenantId
        +String email
        +String fullName
        +List~String~ roles
        +String status
    }

    class AccountantAccess {
        +String id
        +String tenantId
        +String accountantUserId
        +Boolean readOnly
        +String status
        +Instant grantedAt
    }

    class FiscalProfile {
        +String tenantId
        +TaxRegime taxRegime
        +BigDecimal estimatedTaxRate
        +String notes
    }

    class TaxRegime {
        <<enumeration>>
        SIMPLES_NACIONAL
        LUCRO_PRESUMIDO
        LUCRO_REAL
    }

    class ReportEntry {
        +String tenantId
        +String platform
        +String orderId
        +LocalDate saleDate
        +BigDecimal grossValue
        +BigDecimal receivedValue
        +BigDecimal feeValue
        +BigDecimal receivableValue
        +ReportEntryStatus status
    }

    class ExpenseEntry {
        +String tenantId
        +LocalDate expenseDate
        +ExpenseCategory category
        +String description
        +BigDecimal amount
        +ExpenseAttachment attachment
    }

    class StockItem {
        +String tenantId
        +String sku
        +String description
        +BigDecimal unitCost
        +BigDecimal quantity
    }

    class StockMovement {
        +String tenantId
        +String stockItemId
        +String movementType
        +BigDecimal quantity
        +BigDecimal unitCost
        +String referenceType
        +String referenceId
    }

    class PurchaseEntry {
        +String tenantId
        +String nfeNumber
        +String supplierName
        +LocalDate issueDate
        +BigDecimal totalCost
    }

    class BankTransaction {
        +String tenantId
        +String fitId
        +String tranType
        +BigDecimal amount
        +BankTransactionCategory category
        +LocalDate postedDate
    }

    class DreStatement {
        +BigDecimal grossRevenue
        +BigDecimal marketplaceFees
        +BigDecimal estimatedTaxes
        +BigDecimal cmv
        +BigDecimal operatingExpenses
        +BigDecimal bankingExpenses
        +BigDecimal netResult
        +BigDecimal distributableProfit
    }

    class AccountingPeriodClosing {
        +String tenantId
        +YearMonth periodMonth
        +String signedByUserId
        +String signedByEmail
        +String signatureHash
        +BigDecimal distributableProfit
    }

    class ProfitDistribution {
        +String tenantId
        +YearMonth periodMonth
        +BigDecimal amount
        +LocalDate distributedAt
        +String recipientName
        +String createdByEmail
    }

    Tenant "1" --> "*" UserAccount
    Tenant "1" --> "*" AccountantAccess
    UserAccount "1" --> "*" AccountantAccess : accountant
    Tenant "1" --> "1" FiscalProfile
    FiscalProfile --> TaxRegime
    Tenant "1" --> "*" ReportEntry
    Tenant "1" --> "*" ExpenseEntry
    Tenant "1" --> "*" StockItem
    StockItem "1" --> "*" StockMovement
    PurchaseEntry "1" --> "*" StockMovement : ENTRY
    ReportEntry "1" --> "*" StockMovement : EXIT / SALE_REVERSAL
    Tenant "1" --> "*" BankTransaction
    DreStatement ..> ReportEntry : aggregates
    DreStatement ..> ExpenseEntry : aggregates
    DreStatement ..> StockMovement : CMV
    DreStatement ..> BankTransaction : banking expenses
    AccountingPeriodClosing ..> DreStatement : freezes result
    AccountingPeriodClosing "1" --> "*" ProfitDistribution
```

## 5. Planta UML - Servicos e Casos de Uso

```mermaid
classDiagram
    direction TB

    class ReportingResource {
        +dre(tenantId, from, to)
        +signAccountingClosing(tenantId, month)
        +batchSignAccountingClosings(month, tenantIds)
        +profitAvailability(tenantId)
        +createProfitDistribution(tenantId)
        +importNfeXml(tenantId, xml)
        +importOfx(tenantId, ofx)
    }

    class FiscalAccountingService {
        +dre(tenantId, from, to) DreStatement
        +signClosing(tenantId, month, signer, hash)
        +profitAvailability(tenantId) ProfitAvailability
        +createProfitDistribution(command)
        -taxEstimate(regime, grossRevenue, resultBeforeTaxes)
        -simplesTaxEstimate()
        -lucroPresumidoTaxEstimate()
        -lucroRealTaxEstimate()
    }

    class StockService {
        +importNfeXml(tenantId, xml) PurchaseEntry
        +upsertStockItem(tenantId, sku, description, unitCost)
        +recordSaleMovement(tenantId, sku, quantity, orderId, saleDate)
        +reverseSaleMovements(tenantId, orderId, saleDate)
    }

    class BankTransactionService {
        +importOfx(tenantId, ofx) List~BankTransaction~
        +listByPeriod(tenantId, from, to)
    }

    class TenantAuthorizationService {
        +requireReadable(auth, tenantId)
        +requireWritable(auth, tenantId)
        +requireClosingSigner(auth, tenantId)
        +requireBatchClosingSigner(auth, tenantIds)
    }

    class ReportingService {
        +summary(tenantId, filters) FinancialSummary
        +entries(tenantId, filters) ReportEntryPage
        +upsert(command) ReportEntry
    }

    class CoreConnectorService {
        +syncAll(connectorName, tenantId)
        +normalizeFees()
        +publishReportEntryEvent()
    }

    ReportingResource --> TenantAuthorizationService
    ReportingResource --> FiscalAccountingService
    ReportingResource --> StockService
    ReportingResource --> BankTransactionService
    ReportingResource --> ReportingService
    FiscalAccountingService --> ReportingService
    FiscalAccountingService --> StockService
    FiscalAccountingService --> BankTransactionService
    CoreConnectorService ..> ReportingResource : internal entries
```

## 6. Sequencia - Venda com CMV e Estorno

```mermaid
sequenceDiagram
    actor Seller as Lojista
    participant Client as Next Client
    participant Core as core-service
    participant Mkt as Marketplace API
    participant Report as reporting-service
    participant Stock as StockService
    participant DRE as FiscalAccountingService

    Seller->>Client: Sincronizar marketplace
    Client->>Core: POST /api/core/connectors/{name}/sync-all
    Core->>Mkt: Buscar pedidos, pagamentos e taxas
    Mkt-->>Core: Pedidos com itens, status, taxas e frete
    Core->>Core: Normaliza gross, fee, shipping_cost e items
    Core->>Report: POST /reports/internal/entries
    Report->>Report: Upsert ReportEntry

    alt Pedido pago/recebido
        Report->>Stock: recordSaleMovement(SKU, quantity)
        Stock->>Stock: Cria movimento EXIT
    else Pedido cancelado/reembolsado
        Report->>Stock: reverseSaleMovements(orderId)
        Stock->>Stock: Cria SALE_REVERSAL e devolve estoque
    end

    Seller->>Client: Abrir DRE
    Client->>Report: GET /api/reports/tenants/{id}/dre
    Report->>DRE: Calcular DRE
    DRE->>DRE: Receita - taxas/frete - impostos - CMV - despesas - banco
    DRE-->>Client: DreStatement
```

## 7. Sequencia - BPO em Lote

```mermaid
sequenceDiagram
    actor Accountant as Contador
    participant Client as Next Client /bpo
    participant User as user-service
    participant Report as reporting-service
    participant Authz as TenantAuthorizationService
    participant Fiscal as FiscalAccountingService

    Accountant->>Client: Acessa painel BPO
    Client->>User: GET /api/users/accountant/clients
    User-->>Client: Clientes vinculados

    loop Para cada cliente
        Client->>Report: GET /summary
        Client->>Report: GET /dre
        Client->>Report: GET /closings/{month}
    end

    Client->>Client: Calcula pendentes elegiveis
    Accountant->>Client: Clica Assinar pendentes
    Client->>Report: POST /reports/bpo/closings/{month}/batch-sign
    Report->>Authz: requireBatchClosingSigner(jwt, tenantIds)
    Authz-->>Report: OK se todos estao na carteira do contador

    loop Para cada tenant do lote
        Report->>Fiscal: closing(tenantId, month)
        alt Ja fechado
            Fiscal-->>Report: Existing closing
            Report-->>Report: Resultado SKIPPED
        else Aberto
            Report->>Fiscal: signClosing(tenantId, month, signer, hash)
            Fiscal->>Fiscal: Calcula DRE e distributable_profit
            Fiscal->>Fiscal: Persiste AccountingPeriodClosing
            Report-->>Report: Resultado SIGNED
        end
    end

    Report-->>Client: signed_count, skipped_count, failed_count
    Client->>Client: Revalida /bpo e mostra resultado do lote
```

## 8. Sequencia - Lucro Disponivel

```mermaid
sequenceDiagram
    actor Accountant as Contador
    actor Seller as Lojista
    participant Client as Next Client
    participant Report as reporting-service
    participant Fiscal as FiscalAccountingService

    Accountant->>Client: Revisa DRE do periodo
    Accountant->>Report: POST /closings/{month}/sign
    Report->>Fiscal: signClosing()
    Fiscal->>Fiscal: Calcula distributable_profit
    Fiscal->>Fiscal: Imutabiliza periodo
    Fiscal-->>Report: AccountingPeriodClosing

    Seller->>Client: Abre DRE
    Client->>Report: GET /profit/available
    Report->>Fiscal: profitAvailability()
    Fiscal-->>Client: total_released_profit, distributed_profit, available_profit

    Seller->>Client: Registra retirada
    Client->>Report: POST /profit/distributions
    Report->>Fiscal: createProfitDistribution()
    Fiscal->>Fiscal: Valida saldo disponivel
    Fiscal-->>Client: ProfitDistribution
```

## 9. Estado de Permissao do Contador

```mermaid
stateDiagram-v2
    [*] --> LoggedOut
    LoggedOut --> Authenticated: login

    Authenticated --> SellerAdmin: role ADMIN ou VENDEDOR
    Authenticated --> AccountantReadOnly: role CONTADOR sem ADMIN
    Authenticated --> AccountantBPO: role CONTADOR
    Authenticated --> AccountantBPO: role BPO_ADMIN ou ADMIN+CONTADOR

    SellerAdmin --> OperateTenant: criar despesas, conectar marketplace, importar XML/OFX, editar fiscal, retirar lucro
    AccountantReadOnly --> ViewTenant: visualizar dashboard, lancamentos, despesas, estoque, extrato, DRE, configuracoes
    AccountantReadOnly --> LockedActions: acoes bloqueadas com cadeado animado
    AccountantBPO --> OperateBPO: visualizar clientes vinculados ou carteira global e assinar fechamentos em lote

    LockedActions --> ViewTenant: continua navegando em modo leitura
```

## 10. Regras de Demonstracao ao Cliente

Para demonstrar valor sem prometer itens ainda pendentes, use este roteiro:

1. Conectores: mostrar marketplace conectado e sync.
2. Lancamentos: mostrar vendas normalizadas com status.
3. Estoque: mostrar SKU, custo unitario e entrada via XML.
4. DRE: mostrar receita, taxas/frete, impostos automaticos, CMV, banco e lucro liquido.
5. Configuracoes: trocar regime tributario e observar aliquota efetiva na DRE.
6. Extrato: importar OFX e mostrar despesas bancarias entrando na DRE.
7. Fechamento: contador assina periodo.
8. Lucro disponivel: lojista visualiza saldo liberado e registra distribuicao.
9. BPO: contador acessa multiplos clientes e assina pendentes em lote.
10. Modo contador: abrir telas operacionais e mostrar cadeado animado bloqueando escrita.

## 11. Pendencias Tecnicas Relevantes

| Pendencia | Motivo | Prioridade |
|-----------|--------|------------|
| Balanco Patrimonial | Necessario para fechar ciclo contabil completo com Ativo, Passivo e PL. | Alta |
| NF-e/SEFAZ emissao real | Hoje o sistema usa XML/rastreio; nao emite nota nem calcula imposto por NF-e emitida. | Media/Alta |
| Open Finance real | OFX funciona, mas integracao bancaria automatica ainda falta. | Media |
| Webhooks marketplace em tempo real | Cancelamentos funcionam por sync/conector; webhook reduz latencia. | Media |
| ICP-Brasil A1/A3 nativo | Clicksign existe; assinatura nativa ainda nao. | Baixa/Media |

## 12. Observacoes de Deploy

Deploys recentes realizados:

| Servico | Mudanca | Status |
|---------|---------|--------|
| core-service | Padronizacao fina de taxas/frete por marketplace. | Deploy realizado. |
| reporting-service | Lucro disponivel, regime tributario automatico e calculo DRE atualizado. | Deploy realizado. |
| user-service/auth-service | CNPJ/BrasilAPI e ajustes de perfil externo. | Deploy backend realizado anteriormente. |
| apps/client | Bloqueio visual do contador e painel BPO em lote. | Implementado localmente; deploy deve ser feito quando aprovado. |
