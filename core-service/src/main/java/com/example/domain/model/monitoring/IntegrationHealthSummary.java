package com.example.domain.model.monitoring;

import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.IntegrationHealthStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record IntegrationHealthSummary(
        @JsonProperty("integration_name") String integrationName,
        @JsonProperty("current_status") IntegrationHealthStatus currentStatus,
        @JsonProperty("last_check_at") Instant lastCheckAt,
        @JsonProperty("last_success_at") Instant lastSuccessAt,
        @JsonProperty("last_failure_at") Instant lastFailureAt,
        @JsonProperty("last_failure_type") ApiFailureType lastFailureType,
        @JsonProperty("avg_response_time_ms") Integer avgResponseTimeMs,
        @JsonProperty("requests_24h") int requests24h,
        @JsonProperty("failures_24h") int failures24h,
        @JsonProperty("availability_pct_24h") BigDecimal availabilityPct24h) {

    public static IntegrationHealthSummary unknown(String integrationName) {
        return new IntegrationHealthSummary(
                integrationName,
                IntegrationHealthStatus.UP,
                null, null, null, null, null,
                0, 0, null);
    }
}
