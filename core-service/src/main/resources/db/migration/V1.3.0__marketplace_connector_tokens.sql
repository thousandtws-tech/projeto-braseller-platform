CREATE TABLE marketplace_connector_tokens (
    tenant_id VARCHAR(80) NOT NULL,
    connector_name VARCHAR(80) NOT NULL,
    seller_id VARCHAR(80),
    access_token TEXT NOT NULL,
    refresh_token TEXT NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, connector_name)
);

CREATE INDEX idx_marketplace_connector_tokens_connector
    ON marketplace_connector_tokens(connector_name);
