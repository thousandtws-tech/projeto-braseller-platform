package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ClicksignWebhookEvent(
        @JsonProperty("id") String id,
        @JsonProperty("event_name") String eventName,
        @JsonProperty("account_key") String accountKey,
        @JsonProperty("envelope_id") String envelopeId,
        @JsonProperty("document_key") String documentKey,
        @JsonProperty("occurred_at") Instant occurredAt,
        @JsonProperty("processing_status") String processingStatus,
        @JsonProperty("processing_message") String processingMessage,
        @JsonProperty("received_at") Instant receivedAt,
        @JsonProperty("processed_at") Instant processedAt) {
}
