package com.example.application.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

public record DreCalculationRequestedEvent(
        @JsonProperty("event_id") String eventId,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("job_id") String jobId,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("from") LocalDate from,
        @JsonProperty("to") LocalDate to,
        @JsonProperty("requested_by_user_id") String requestedByUserId,
        @JsonProperty("requested_by_email") String requestedByEmail,
        @JsonProperty("requested_at") Instant requestedAt) {

    public static DreCalculationRequestedEvent create(
            String tenantId,
            LocalDate from,
            LocalDate to,
            String requestedByUserId,
            String requestedByEmail) {
        return new DreCalculationRequestedEvent(
                UUID.randomUUID().toString(),
                "reporting.dre-calculation-requested.v1",
                UUID.randomUUID().toString(),
                tenantId,
                from,
                to,
                requestedByUserId,
                requestedByEmail,
                Instant.now()
        );
    }
}
