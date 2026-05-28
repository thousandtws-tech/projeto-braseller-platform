package com.example.domain.model;

import java.time.Instant;

public record BillingWebhookEvent(
        String id,
        BillingProvider provider,
        String providerEventId,
        String tenantId,
        BillingWebhookEventType eventType,
        String status,
        Instant receivedAt,
        String payload) {
}
