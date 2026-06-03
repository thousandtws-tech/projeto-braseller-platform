CREATE TABLE notification_new_sale_events (
    event_id VARCHAR(80) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    marketplace VARCHAR(120) NOT NULL,
    order_id VARCHAR(120) NOT NULL,
    amount DECIMAL(19, 2) NOT NULL DEFAULT 0,
    occurred_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_notification_new_sale_events_tenant_occurred
    ON notification_new_sale_events(tenant_id, occurred_at DESC);

CREATE TABLE notification_new_sale_summaries (
    tenant_id VARCHAR(80) PRIMARY KEY,
    sale_count BIGINT NOT NULL DEFAULT 0,
    gross_revenue DECIMAL(19, 2) NOT NULL DEFAULT 0,
    last_marketplace VARCHAR(120),
    last_order_id VARCHAR(120),
    last_event_id VARCHAR(80),
    last_event_at TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
