package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ConnectorStatus(
        @JsonProperty("platform") String platform,
        @JsonProperty("status") ConnectorConnectionStatus status,
        @JsonProperty("message") String message,
        @JsonProperty("checked_at") Instant checkedAt) {
}
