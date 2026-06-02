CREATE TABLE clicksign_webhook_events (
    id VARCHAR(36) PRIMARY KEY,
    event_name VARCHAR(80) NOT NULL,
    account_key VARCHAR(80),
    envelope_id VARCHAR(80),
    document_key VARCHAR(80),
    event_occurred_at TIMESTAMP,
    payload_json TEXT NOT NULL,
    content_hmac VARCHAR(120),
    processing_status VARCHAR(40) NOT NULL,
    processing_message VARCHAR(300),
    received_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at TIMESTAMP
);

CREATE INDEX idx_clicksign_webhook_events_event_name
    ON clicksign_webhook_events(event_name);

CREATE INDEX idx_clicksign_webhook_events_document_key
    ON clicksign_webhook_events(document_key);

CREATE INDEX idx_clicksign_webhook_events_received_at
    ON clicksign_webhook_events(received_at);
