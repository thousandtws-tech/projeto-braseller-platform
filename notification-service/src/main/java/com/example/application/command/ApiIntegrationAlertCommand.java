package com.example.application.command;

import java.time.Instant;

public record ApiIntegrationAlertCommand(
        String eventId,
        String eventType,
        Instant occurredAt,
        String tenantId,
        String recipientEmail,
        String integrationName,
        String endpoint,
        String failureType,
        String severity,
        String impact,
        String actionTaken) {
}
