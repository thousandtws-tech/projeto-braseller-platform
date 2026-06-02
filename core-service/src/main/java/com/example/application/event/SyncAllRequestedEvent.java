package com.example.application.event;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.UUID;

public record SyncAllRequestedEvent(
        @JsonProperty("event_id") String eventId,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("connector_name") String connectorName,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("recipient_email") String recipientEmail,
        @JsonProperty("since") Instant since,
        @JsonProperty("requested_at") Instant requestedAt) {

    public static SyncAllRequestedEvent create(String connectorName, String tenantId, String recipientEmail, Instant since) {
        return new SyncAllRequestedEvent(
                UUID.randomUUID().toString(),
                "core.sync-all-requested.v1",
                connectorName,
                tenantId,
                recipientEmail,
                since,
                Instant.now()
        );
    }
}
