package com.example.domain.model;

import java.time.Instant;

public record NotificationMessage(
        String id,
        String tenantId,
        NotificationType type,
        String title,
        String message,
        String recipientEmail,
        NotificationChannel channel,
        NotificationStatus status,
        Instant readAt,
        Instant createdAt,
        String severity) {
}
