package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record AccountingPeriodClosing(
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("period_month") String periodMonth,
        @JsonProperty("signed_by_user_id") String signedByUserId,
        @JsonProperty("signed_by_email") String signedByEmail,
        @JsonProperty("signature_hash") String signatureHash,
        @JsonProperty("signed_at") Instant signedAt) {
}
