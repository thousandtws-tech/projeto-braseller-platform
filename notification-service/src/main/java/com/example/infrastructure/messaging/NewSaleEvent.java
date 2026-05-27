package com.example.infrastructure.messaging;

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
}
