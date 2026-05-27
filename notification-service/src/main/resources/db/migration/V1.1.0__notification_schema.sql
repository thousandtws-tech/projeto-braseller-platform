CREATE TABLE notification_preferences (
    tenant_id VARCHAR(80) PRIMARY KEY,
    email_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    new_sale_enabled BOOLEAN NOT NULL DEFAULT FALSE,
    monthly_closing_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    ml_payment_release_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    weekly_accountant_report_enabled BOOLEAN NOT NULL DEFAULT TRUE,
    accountant_email VARCHAR(180),
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notifications (
    id VARCHAR(80) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    type VARCHAR(80) NOT NULL,
    title VARCHAR(180) NOT NULL,
    message VARCHAR(2000) NOT NULL,
    recipient_email VARCHAR(180),
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    read_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE notification_deliveries (
    id VARCHAR(80) PRIMARY KEY,
    notification_id VARCHAR(80) NOT NULL REFERENCES notifications(id) ON DELETE CASCADE,
    channel VARCHAR(32) NOT NULL,
    status VARCHAR(32) NOT NULL,
    error_message VARCHAR(1000),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);
