ALTER TABLE user_accounts ALTER COLUMN email_verified SET DEFAULT FALSE;

CREATE INDEX idx_user_accounts_email_verified_status
    ON user_accounts (email_verified, status);
