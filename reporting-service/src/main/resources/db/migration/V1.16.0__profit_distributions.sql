CREATE TABLE profit_distributions (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(36) NOT NULL,
    period_month DATE NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    distributed_at DATE NOT NULL,
    recipient_name VARCHAR(160),
    notes VARCHAR(500),
    created_by_user_id VARCHAR(80) NOT NULL,
    created_by_email VARCHAR(180) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_profit_distributions_closing
        FOREIGN KEY (tenant_id, period_month)
        REFERENCES accounting_period_closings(tenant_id, period_month),
    CONSTRAINT chk_profit_distributions_amount_positive CHECK (amount > 0)
);

CREATE INDEX idx_profit_distributions_tenant_period
    ON profit_distributions(tenant_id, period_month);

CREATE INDEX idx_profit_distributions_tenant_created_at
    ON profit_distributions(tenant_id, created_at);
