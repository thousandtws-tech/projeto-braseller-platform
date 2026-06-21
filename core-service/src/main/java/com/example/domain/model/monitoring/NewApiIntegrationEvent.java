package com.example.domain.model.monitoring;

import com.example.domain.enums.ApiCallOutcome;
import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;

import java.time.Instant;

public record NewApiIntegrationEvent(
        String tenantId,
        String integrationName,
        String endpoint,
        String operation,
        Instant occurredAt,
        Integer responseTimeMs,
        Integer httpStatus,
        ApiCallOutcome outcome,
        ApiFailureType failureType,
        ApiSeverity severity,
        String impact,
        String actionTaken,
        String errorMessage) {
}
