package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record ConnectorToken(
        @JsonProperty("platform") String platform,
        @JsonProperty("status") ConnectorConnectionStatus status,
        @JsonProperty("expires_at") Instant expiresAt) {
}
