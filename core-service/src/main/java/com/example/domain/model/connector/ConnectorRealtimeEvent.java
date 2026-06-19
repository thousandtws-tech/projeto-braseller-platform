package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;
import java.util.Map;

public record ConnectorRealtimeEvent(
        @JsonProperty("sequence") long sequence,
        @JsonProperty("event_id") String eventId,
        @JsonProperty("event_type") String eventType,
        @JsonProperty("aggregate_id") String aggregateId,
        @JsonProperty("occurred_at") Instant occurredAt,
        @JsonProperty("payload") Map<String, Object> payload) {
}
