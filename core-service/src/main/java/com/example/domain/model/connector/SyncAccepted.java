package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record SyncAccepted(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("status") String status,
        @JsonProperty("connector_name") String connectorName,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("since") Instant since,
        @JsonProperty("queued_at") Instant queuedAt) {
}
