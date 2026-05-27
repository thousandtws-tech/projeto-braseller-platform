CREATE INDEX idx_notifications_tenant_created_at
    ON notifications (tenant_id, created_at DESC);

CREATE INDEX idx_notifications_tenant_status
    ON notifications (tenant_id, status);

CREATE INDEX idx_notification_deliveries_notification
    ON notification_deliveries (notification_id);
