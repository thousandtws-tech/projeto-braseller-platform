CREATE TABLE api_integration_events (
    id                VARCHAR(36)  PRIMARY KEY,
    tenant_id         VARCHAR(80)  NOT NULL,
    integration_name  VARCHAR(60)  NOT NULL,
    endpoint          VARCHAR(300) NOT NULL,
    operation         VARCHAR(120),
    occurred_at       TIMESTAMP    NOT NULL,
    response_time_ms  INTEGER,
    http_status       INTEGER,
    outcome           VARCHAR(20)  NOT NULL,
    failure_type      VARCHAR(40),
    severity          VARCHAR(20)  NOT NULL,
    impact            VARCHAR(400),
    action_taken      VARCHAR(400),
    error_message     VARCHAR(1000),
    created_at        TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_api_int_events_tenant_integration_time
    ON api_integration_events(tenant_id, integration_name, occurred_at DESC);

CREATE INDEX idx_api_int_events_severity
    ON api_integration_events(severity, occurred_at DESC);

CREATE TABLE api_integration_status (
    tenant_id            VARCHAR(80) NOT NULL,
    integration_name     VARCHAR(60) NOT NULL,
    current_status       VARCHAR(20) NOT NULL DEFAULT 'UP',
    last_check_at        TIMESTAMP,
    last_success_at      TIMESTAMP,
    last_failure_at      TIMESTAMP,
    last_failure_type    VARCHAR(40),
    consecutive_failures INTEGER NOT NULL DEFAULT 0,
    avg_response_time_ms INTEGER,
    requests_24h         INTEGER NOT NULL DEFAULT 0,
    failures_24h         INTEGER NOT NULL DEFAULT 0,
    availability_pct_24h NUMERIC(5,2),
    updated_at           TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id, integration_name)
);
