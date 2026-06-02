package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;

public record DreCalculationJob(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("from") LocalDate from,
        @JsonProperty("to") LocalDate to,
        @JsonProperty("status") DreCalculationStatus status,
        @JsonProperty("requested_by_user_id") String requestedByUserId,
        @JsonProperty("requested_by_email") String requestedByEmail,
        @JsonProperty("requested_at") Instant requestedAt,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("finished_at") Instant finishedAt,
        @JsonProperty("error_message") String errorMessage,
        @JsonProperty("statement") DreStatement statement) {
}
