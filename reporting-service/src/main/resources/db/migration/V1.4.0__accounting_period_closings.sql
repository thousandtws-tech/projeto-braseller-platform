CREATE TABLE accounting_period_closings (
    tenant_id VARCHAR(80) NOT NULL,
    period_month DATE NOT NULL,
    signed_by_user_id VARCHAR(80) NOT NULL,
    signed_by_email VARCHAR(180) NOT NULL,
    signature_hash VARCHAR(180) NOT NULL,
    signed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, period_month)
);

CREATE INDEX idx_accounting_period_closings_signed_at
    ON accounting_period_closings(signed_at);
