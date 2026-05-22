CREATE INDEX idx_user_accounts_tenant_status ON user_accounts(tenant_id, status);
CREATE INDEX idx_user_accounts_tenant_created_at ON user_accounts(tenant_id, created_at);
CREATE INDEX idx_accountant_access_accountant_user_id ON accountant_access(accountant_user_id);
CREATE INDEX idx_accountant_access_granted_by_user_id ON accountant_access(granted_by_user_id);
CREATE INDEX idx_accountant_access_tenant_status ON accountant_access(tenant_id, status);
