package com.example.application.command;

import java.math.BigDecimal;
import java.time.Instant;

public record NewSaleNotificationCommand(
        String eventId,
        String eventType,
        Instant occurredAt,
        String tenantId,
        String recipientEmail,
        String marketplace,
        String orderId,
        BigDecimal amount) {
    public NewSaleNotificationCommand(
            String tenantId,
            String recipientEmail,
            String marketplace,
            String orderId,
            BigDecimal amount) {
        this(null, null, null, tenantId, recipientEmail, marketplace, orderId, amount);
    }
}
