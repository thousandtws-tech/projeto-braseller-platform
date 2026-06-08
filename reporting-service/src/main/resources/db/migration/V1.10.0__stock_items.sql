CREATE TABLE stock_items (
    id             VARCHAR(36)     NOT NULL,
    tenant_id      VARCHAR(80)     NOT NULL,
    sku            VARCHAR(80)     NOT NULL,
    description    VARCHAR(255)    NOT NULL,
    unit_cost      DECIMAL(15, 2)  NOT NULL,
    quantity       DECIMAL(15, 4)  NOT NULL DEFAULT 0,
    created_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (tenant_id, sku)
);

CREATE INDEX idx_stock_items_tenant ON stock_items (tenant_id);
