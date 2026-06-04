# BraSeller Engineering Principles

BraSeller is not only a sales dashboard. It is a tax optimization and accounting evidence engine disguised as a sales manager.

The product promise is simple: transform marketplace, bank, invoice, cost and expense data into a clean, defensible DRE so the seller and accountant can prove real profit and safely distribute profits.

## Non-negotiables

1. The Core does not know marketplace details.
   - Application/domain code must talk to connector ports and normalized contracts.
   - Marketplace-specific API fields, statuses, fees, paging and rate-limit behavior belong in connector implementations.
   - No rule like `if platform == mercado-livre` in application/domain layers.

2. Money is exact.
   - Java monetary values use `BigDecimal`.
   - SQL monetary columns use `DECIMAL` or `NUMERIC`.
   - Do not use `float`, `double`, `REAL`, `FLOAT` or `DOUBLE PRECISION` for financial data.

3. Proof controls accounting eligibility.
   - Manual expenses require an attachment.
   - DRE/deductible expense calculations must only count expenses with valid proof.
   - User-facing flows should block missing proof before submitting to the API.

4. Closed periods are immutable.
   - Once the accountant signs a month, that month is locked.
   - New corrections after closing must be represented in a later open period.
   - Create, update and delete operations touching a closed accounting month must fail.

5. Direct integrations are strategic.
   - Prefer pulling financial split data directly from marketplaces instead of relying on ERP summaries.
   - Webhooks, idempotency, outbox processing and rate-limit handling are core infrastructure concerns, not optional polish.

## Current Guardrails

- `reporting-service/src/test/java/com/example/ArchitectureGuardTest.java` blocks floating point types in reporting code and migrations.
- `core-service/src/test/java/com/example/ConnectorArchitectureGuardTest.java` blocks marketplace-specific logic in core application/domain layers.
- Reporting tests cover required expense attachments and locked accounting periods.
