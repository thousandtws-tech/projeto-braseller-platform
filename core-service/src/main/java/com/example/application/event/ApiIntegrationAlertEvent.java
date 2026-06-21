package com.example.application.event;

import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;

import java.time.Instant;

public record ApiIntegrationAlertEvent(
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

    public static ApiIntegrationAlertEvent create(
            String eventId,
            String tenantId,
            String recipientEmail,
            String integrationName,
            String endpoint,
            ApiFailureType failureType,
            ApiSeverity severity,
            String impact,
            String actionTaken) {
        return new ApiIntegrationAlertEvent(
                eventId,
                "notification.api-integration-alert.v1",
                Instant.now(),
                tenantId,
                recipientEmail,
                integrationName,
                endpoint,
                failureType,
                severity,
                impact,
                actionTaken
        );
    }
}
