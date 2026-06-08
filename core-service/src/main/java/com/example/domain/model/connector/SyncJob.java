package com.example.domain.model.connector;

import com.example.domain.enums.SyncJobStatus;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record SyncJob(
        @JsonProperty("job_id") String jobId,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("connector_name") String connectorName,
        @JsonProperty("since") Instant since,
        @JsonProperty("status") SyncJobStatus status,
        @JsonProperty("recipient_email") String recipientEmail,
        @JsonProperty("requested_at") Instant requestedAt,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("finished_at") Instant finishedAt,
        @JsonProperty("error_message") String errorMessage,
        @JsonProperty("orders_synced") Integer ordersSynced,
        @JsonProperty("payments_synced") Integer paymentsSynced,
        @JsonProperty("fees_synced") Integer feesSynced) {
}
