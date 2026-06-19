CREATE TABLE email_verification_codes (
    user_id VARCHAR(36) PRIMARY KEY REFERENCES user_accounts(id) ON DELETE CASCADE,
    email_normalized VARCHAR(320) NOT NULL UNIQUE,
    code_hash VARCHAR(128) NOT NULL,
    code_salt VARCHAR(64) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    attempts_remaining INTEGER NOT NULL,
    last_sent_at TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_email_verification_codes_email ON email_verification_codes(email_normalized);
