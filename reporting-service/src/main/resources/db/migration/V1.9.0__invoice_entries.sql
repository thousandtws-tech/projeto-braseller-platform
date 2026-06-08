CREATE TABLE invoice_entries (
    id             VARCHAR(36)  NOT NULL,
    tenant_id      VARCHAR(80)  NOT NULL,
    platform       VARCHAR(40)  NOT NULL,
    order_id       VARCHAR(80)  NOT NULL,
    invoice_number VARCHAR(80)  NOT NULL,
    access_key     VARCHAR(50),
    issued_at      DATE,
    status         VARCHAR(20)  NOT NULL DEFAULT 'issued',
    created_at     DATE         NOT NULL DEFAULT CURRENT_DATE,
    PRIMARY KEY (id),
    UNIQUE (tenant_id, platform, order_id)
);

CREATE INDEX idx_invoice_entries_tenant_period
    ON invoice_entries (tenant_id, issued_at DESC);

CREATE INDEX idx_invoice_entries_invoice_number
    ON invoice_entries (tenant_id, invoice_number);
