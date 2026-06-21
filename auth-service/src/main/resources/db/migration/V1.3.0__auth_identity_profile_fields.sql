ALTER TABLE auth_identities ADD COLUMN preferred_username VARCHAR(160);
ALTER TABLE auth_identities ADD COLUMN first_name VARCHAR(80);
ALTER TABLE auth_identities ADD COLUMN last_name VARCHAR(80);
ALTER TABLE auth_identities ADD COLUMN picture_url VARCHAR(1024);
ALTER TABLE auth_identities ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE auth_identities ADD COLUMN accountant_tenant_ids VARCHAR(2000);

CREATE INDEX idx_auth_identities_email_verified_status
    ON auth_identities (email_verified, status);
