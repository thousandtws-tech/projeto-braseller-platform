package com.example.infrastructure.client;

import java.math.BigDecimal;
import java.time.Instant;

public record NewSaleNotificationRequest(
        String eventId,
        String eventType,
        Instant occurredAt,
        String tenantId,
        String recipientEmail,
        String marketplace,
        String orderId,
        BigDecimal amount) {
}
