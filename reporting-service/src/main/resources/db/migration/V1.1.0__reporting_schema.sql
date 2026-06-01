CREATE TABLE report_entries (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    platform VARCHAR(80) NOT NULL,
    order_id VARCHAR(120) NOT NULL,
    sale_date DATE NOT NULL,
    gross_value DECIMAL(10, 2) NOT NULL DEFAULT 0,
    received_value DECIMAL(10, 2) NOT NULL DEFAULT 0,
    fee_value DECIMAL(10, 2) NOT NULL DEFAULT 0,
    receivable_value DECIMAL(10, 2) NOT NULL DEFAULT 0,
    payment_method VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    release_date DATE,
    buyer_name VARCHAR(180),
    invoice_number VARCHAR(120),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_report_entries_tenant_platform_order UNIQUE (tenant_id, platform, order_id)
);
