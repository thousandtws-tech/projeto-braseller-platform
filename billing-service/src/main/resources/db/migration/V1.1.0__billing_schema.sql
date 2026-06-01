CREATE TABLE billing_plans (
    code VARCHAR(40) PRIMARY KEY,
    name VARCHAR(80) NOT NULL,
    description VARCHAR(240) NOT NULL,
    monthly_price DECIMAL(10, 2) NOT NULL,
    currency VARCHAR(3) NOT NULL,
    trial_days INTEGER NOT NULL,
    marketplace_limit INTEGER NOT NULL,
    user_limit INTEGER NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    sort_order INTEGER NOT NULL
);

CREATE TABLE billing_subscriptions (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL UNIQUE,
    plan_code VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    provider_customer_id VARCHAR(160),
    provider_subscription_id VARCHAR(160),
    trial_started_at TIMESTAMP,
    trial_ends_at TIMESTAMP,
    current_period_started_at TIMESTAMP,
    current_period_ends_at TIMESTAMP,
    suspended_at TIMESTAMP,
    cancellation_reason VARCHAR(240),
    latest_event_id VARCHAR(160),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_billing_subscriptions_plan FOREIGN KEY (plan_code) REFERENCES billing_plans(code)
);

CREATE TABLE billing_webhook_events (
    id VARCHAR(36) PRIMARY KEY,
    provider VARCHAR(40) NOT NULL,
    provider_event_id VARCHAR(160) NOT NULL,
    tenant_id VARCHAR(80) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    status VARCHAR(40) NOT NULL,
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    payload TEXT,
    CONSTRAINT uq_billing_webhook_events_provider_event UNIQUE (provider, provider_event_id)
);

INSERT INTO billing_plans
(code, name, description, monthly_price, currency, trial_days, marketplace_limit, user_limit, active, sort_order)
VALUES
('BASIC', 'Basico', 'Plano inicial para vendedores que estao validando um marketplace.', 49.90, 'BRL', 14, 1, 2, TRUE, 10),
('PRO', 'Pro', 'Plano para operacao multi-marketplace com automacoes recorrentes.', 149.90, 'BRL', 14, 5, 10, TRUE, 20),
('AGENCY', 'Agencia', 'Plano para agencias e contadores que operam varias contas.', 399.90, 'BRL', 14, 25, 50, TRUE, 30);
