CREATE TABLE tenant_fiscal_profiles (
    tenant_id VARCHAR(80) PRIMARY KEY,
    tax_regime VARCHAR(40) NOT NULL,
    estimated_tax_rate DECIMAL(9, 4) NOT NULL DEFAULT 0,
    notes VARCHAR(500),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE expense_entries (
    id VARCHAR(36) PRIMARY KEY,
    tenant_id VARCHAR(80) NOT NULL,
    expense_date DATE NOT NULL,
    category VARCHAR(60) NOT NULL,
    description VARCHAR(240) NOT NULL,
    amount DECIMAL(10, 2) NOT NULL,
    attachment_public_id VARCHAR(240) NOT NULL,
    attachment_secure_url VARCHAR(600) NOT NULL,
    attachment_resource_type VARCHAR(40),
    attachment_original_filename VARCHAR(240),
    attachment_content_type VARCHAR(120),
    attachment_size_bytes BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_tenant_fiscal_profiles_regime ON tenant_fiscal_profiles(tax_regime);
CREATE INDEX idx_expense_entries_tenant_date ON expense_entries(tenant_id, expense_date);
CREATE INDEX idx_expense_entries_tenant_category ON expense_entries(tenant_id, category);
