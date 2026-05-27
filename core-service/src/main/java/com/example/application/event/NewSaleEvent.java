package com.example.application.event;

import java.math.BigDecimal;
import java.time.Instant;

public record NewSaleEvent(
        String eventId,
        String eventType,
        Instant occurredAt,
        String tenantId,
        String recipientEmail,
        String marketplace,
        String orderId,
        BigDecimal amount) {
    public static NewSaleEvent create(
            String eventId,
            String tenantId,
            String recipientEmail,
            String marketplace,
            String orderId,
            BigDecimal amount) {
        return new NewSaleEvent(
                eventId,
                "notification.new-sale.v1",
                Instant.now(),
                tenantId,
                recipientEmail,
                marketplace,
                orderId,
                amount
        );
    }
}
