ALTER TABLE notifications ADD COLUMN severity VARCHAR(20);
CREATE INDEX idx_notifications_tenant_severity ON notifications(tenant_id, severity);
