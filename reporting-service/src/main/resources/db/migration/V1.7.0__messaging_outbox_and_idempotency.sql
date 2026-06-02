CREATE TABLE messaging_outbox_events (
    id VARCHAR(36) PRIMARY KEY,
    event_type VARCHAR(140) NOT NULL,
    aggregate_id VARCHAR(180) NOT NULL,
    partition_key VARCHAR(180) NOT NULL,
    payload TEXT NOT NULL,
    status VARCHAR(24) NOT NULL DEFAULT 'PENDING',
    attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(600),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    published_at TIMESTAMP
);

CREATE INDEX idx_messaging_outbox_ready
    ON messaging_outbox_events(status, next_attempt_at, created_at);

CREATE INDEX idx_messaging_outbox_type_status
    ON messaging_outbox_events(event_type, status, created_at);
