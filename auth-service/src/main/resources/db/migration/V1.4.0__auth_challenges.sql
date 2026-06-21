CREATE TABLE auth_challenges (
    id VARCHAR(36) PRIMARY KEY,
    challenge_type VARCHAR(40) NOT NULL,
    email VARCHAR(320) NOT NULL,
    email_normalized VARCHAR(320) NOT NULL,
    code_hash VARCHAR(128) NOT NULL,
    expires_at TIMESTAMP NOT NULL,
    used_at TIMESTAMP,
    attempts INTEGER NOT NULL DEFAULT 0,
    subject_exists BOOLEAN NOT NULL DEFAULT TRUE,
    request_ip VARCHAR(128),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_auth_challenges_email_type_open
    ON auth_challenges (email_normalized, challenge_type, used_at, expires_at);

CREATE INDEX idx_auth_challenges_request_ip_created
    ON auth_challenges (request_ip, created_at);

CREATE INDEX idx_auth_challenges_type_created
    ON auth_challenges (challenge_type, created_at);
