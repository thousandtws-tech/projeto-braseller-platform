package com.example.infrastructure.client;

import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;

import java.time.Instant;

public record ApiIntegrationAlertRequest(
        String eventId,
        String eventType,
        Instant occurredAt,
        String tenantId,
        String recipientEmail,
        String integrationName,
        String endpoint,
        ApiFailureType failureType,
        ApiSeverity severity,
        String impact,
        String actionTaken) {
}
