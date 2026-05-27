ALTER TABLE user_accounts ADD COLUMN preferred_username VARCHAR(160);
ALTER TABLE user_accounts ADD COLUMN first_name VARCHAR(80);
ALTER TABLE user_accounts ADD COLUMN last_name VARCHAR(80);
ALTER TABLE user_accounts ADD COLUMN picture_url VARCHAR(1024);
ALTER TABLE user_accounts ADD COLUMN email_verified BOOLEAN NOT NULL DEFAULT TRUE;
