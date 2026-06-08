CREATE TABLE bank_transactions (
    id           VARCHAR(36)    NOT NULL,
    tenant_id    VARCHAR(80)    NOT NULL,
    fit_id       VARCHAR(80),                  -- identificador único do OFX
    tran_type    VARCHAR(20)    NOT NULL,       -- DEBIT, CREDIT
    amount       DECIMAL(15, 2) NOT NULL,
    posted_date  DATE           NOT NULL,
    description  VARCHAR(255),
    category     VARCHAR(40)    NOT NULL DEFAULT 'OUTROS',
    imported_at  TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (id),
    UNIQUE (tenant_id, fit_id)
);

CREATE INDEX idx_bank_transactions_tenant_date ON bank_transactions (tenant_id, posted_date);
