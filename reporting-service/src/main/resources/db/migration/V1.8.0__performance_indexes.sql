CREATE INDEX idx_report_entries_tenant_sale_date_id
    ON report_entries(tenant_id, sale_date DESC, id);

CREATE INDEX idx_report_entries_tenant_platform_sale_date
    ON report_entries(tenant_id, platform, sale_date DESC);

CREATE INDEX idx_report_entries_tenant_status_sale_date
    ON report_entries(tenant_id, status, sale_date DESC);

CREATE INDEX idx_report_entries_tenant_payment_sale_date
    ON report_entries(tenant_id, payment_method, sale_date DESC);

CREATE INDEX idx_report_entries_tenant_release_date
    ON report_entries(tenant_id, release_date ASC);

CREATE INDEX idx_expense_entries_tenant_date_created_id
    ON expense_entries(tenant_id, expense_date DESC, created_at DESC, id);

CREATE INDEX idx_expense_entries_tenant_category_date
    ON expense_entries(tenant_id, category, expense_date DESC);
