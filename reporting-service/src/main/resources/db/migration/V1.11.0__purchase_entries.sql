CREATE TABLE purchase_entries (
    id              VARCHAR(36)    NOT NULL,
    tenant_id       VARCHAR(80)    NOT NULL,
    nfe_number      VARCHAR(40),
    supplier_name   VARCHAR(255),
    issue_date      DATE           NOT NULL,
    total_cost      DECIMAL(15, 2) NOT NULL,
    created_at      TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE TABLE purchase_entry_items (
    id                  VARCHAR(36)    NOT NULL,
    purchase_entry_id   VARCHAR(36)    NOT NULL REFERENCES purchase_entries(id) ON DELETE CASCADE,
    sku                 VARCHAR(80)    NOT NULL,
    description         VARCHAR(255)   NOT NULL,
    quantity            DECIMAL(15, 4) NOT NULL,
    unit_cost           DECIMAL(15, 2) NOT NULL,
    total_cost          DECIMAL(15, 2) NOT NULL,
    PRIMARY KEY (id)
);

CREATE TABLE stock_movements (
    id                  VARCHAR(36)    NOT NULL,
    tenant_id           VARCHAR(80)    NOT NULL,
    stock_item_id       VARCHAR(36)    NOT NULL REFERENCES stock_items(id),
    movement_type       VARCHAR(20)    NOT NULL, -- ENTRY, EXIT
    quantity            DECIMAL(15, 4) NOT NULL,
    unit_cost           DECIMAL(15, 2) NOT NULL,
    reference_id        VARCHAR(80),             -- order_id or purchase_entry_id
    reference_type      VARCHAR(20),             -- SALE or PURCHASE
    movement_date       DATE           NOT NULL,
    created_at          TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id)
);

CREATE INDEX idx_purchase_entries_tenant      ON purchase_entries (tenant_id, issue_date);
CREATE INDEX idx_stock_movements_tenant_date  ON stock_movements  (tenant_id, movement_date);
CREATE INDEX idx_stock_movements_item         ON stock_movements  (stock_item_id);
