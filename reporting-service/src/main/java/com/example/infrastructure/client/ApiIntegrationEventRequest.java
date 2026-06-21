package com.example.infrastructure.client;

import java.time.Instant;

public record ApiIntegrationEventRequest(
        String tenantId,
        String integrationName,
        String endpoint,
        String operation,
        Instant occurredAt,
        Integer responseTimeMs,
        Integer httpStatus,
        String outcome,
        String failureType,
        String severity,
        String impact,
        String actionTaken,
        String errorMessage) {
}
