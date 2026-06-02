CREATE TABLE dre_calculation_jobs (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    period_from DATE NOT NULL,
    period_to DATE NOT NULL,
    status VARCHAR(32) NOT NULL,
    requested_by_user_id VARCHAR(80),
    requested_by_email VARCHAR(180),
    requested_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error_message VARCHAR(600),
    tax_regime VARCHAR(40),
    estimated_tax_rate DECIMAL(9, 4),
    gross_revenue DECIMAL(10, 2),
    marketplace_fees DECIMAL(10, 2),
    estimated_taxes DECIMAL(10, 2),
    operating_expenses DECIMAL(10, 2),
    net_result DECIMAL(10, 2),
    sales_count BIGINT,
    expense_count BIGINT,
    expenses_by_category_json TEXT
);

CREATE INDEX idx_dre_calculation_jobs_tenant_requested
    ON dre_calculation_jobs(tenant_id, requested_at DESC);

CREATE INDEX idx_dre_calculation_jobs_tenant_period
    ON dre_calculation_jobs(tenant_id, period_from, period_to);

CREATE INDEX idx_dre_calculation_jobs_status
    ON dre_calculation_jobs(status);
