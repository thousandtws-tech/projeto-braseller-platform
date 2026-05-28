CREATE INDEX idx_report_entries_tenant_sale_date ON report_entries(tenant_id, sale_date);
CREATE INDEX idx_report_entries_tenant_platform ON report_entries(tenant_id, platform);
CREATE INDEX idx_report_entries_tenant_payment_method ON report_entries(tenant_id, payment_method);
CREATE INDEX idx_report_entries_tenant_status ON report_entries(tenant_id, status);
CREATE INDEX idx_report_entries_tenant_order_id ON report_entries(tenant_id, order_id);
