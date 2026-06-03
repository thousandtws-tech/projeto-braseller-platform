CREATE TABLE processed_message_events (
    event_id VARCHAR(80) PRIMARY KEY,
    event_type VARCHAR(140) NOT NULL,
    status VARCHAR(24) NOT NULL,
    first_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_seen_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP,
    failed_at TIMESTAMP,
    error_message VARCHAR(600)
);

CREATE INDEX idx_processed_message_events_status
    ON processed_message_events(status, last_seen_at);
