package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record SyncResult(
        @JsonProperty("platform") String platform,
        @JsonProperty("orders_synced") int ordersSynced,
        @JsonProperty("payments_synced") int paymentsSynced,
        @JsonProperty("fees_synced") int feesSynced,
        @JsonProperty("started_at") Instant startedAt,
        @JsonProperty("finished_at") Instant finishedAt) {
}
