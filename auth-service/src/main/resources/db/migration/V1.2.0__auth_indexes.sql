CREATE INDEX idx_auth_identities_tenant_status ON auth_identities(tenant_id, status);
CREATE INDEX idx_auth_sessions_tenant_user ON auth_sessions(tenant_id, user_id);
CREATE INDEX idx_auth_sessions_user_active_expiration ON auth_sessions(user_id, revoked_at, expires_at);
