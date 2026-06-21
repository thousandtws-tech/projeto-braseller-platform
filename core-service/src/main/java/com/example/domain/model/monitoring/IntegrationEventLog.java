package com.example.domain.model.monitoring;

import com.example.domain.enums.ApiCallOutcome;
import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record IntegrationEventLog(
        @JsonProperty("id") String id,
        @JsonProperty("integration_name") String integrationName,
        @JsonProperty("occurred_at") Instant occurredAt,
        @JsonProperty("endpoint") String endpoint,
        @JsonProperty("operation") String operation,
        @JsonProperty("response_time_ms") Integer responseTimeMs,
        @JsonProperty("http_status") Integer httpStatus,
        @JsonProperty("outcome") ApiCallOutcome outcome,
        @JsonProperty("failure_type") ApiFailureType failureType,
        @JsonProperty("severity") ApiSeverity severity,
        @JsonProperty("impact") String impact,
        @JsonProperty("action_taken") String actionTaken,
        @JsonProperty("error_message") String errorMessage) {
}
