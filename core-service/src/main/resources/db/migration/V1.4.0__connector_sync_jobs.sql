CREATE TABLE connector_sync_jobs (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    connector_name VARCHAR(80) NOT NULL,
    since_instant TIMESTAMP NOT NULL,
    status VARCHAR(32) NOT NULL,
    recipient_email VARCHAR(180),
    requested_at TIMESTAMP NOT NULL,
    started_at TIMESTAMP,
    finished_at TIMESTAMP,
    error_message VARCHAR(600),
    orders_synced INTEGER,
    payments_synced INTEGER,
    fees_synced INTEGER
);

CREATE INDEX idx_connector_sync_jobs_tenant_requested
    ON connector_sync_jobs(tenant_id, requested_at DESC);

CREATE INDEX idx_connector_sync_jobs_tenant_connector
    ON connector_sync_jobs(tenant_id, connector_name, requested_at DESC);

CREATE INDEX idx_connector_sync_jobs_status
    ON connector_sync_jobs(status);
