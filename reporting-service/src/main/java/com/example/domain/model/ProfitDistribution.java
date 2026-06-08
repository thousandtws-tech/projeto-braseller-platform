package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ProfitDistribution(
        @JsonProperty("id") String id,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("period_month") String periodMonth,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("distributed_at") LocalDate distributedAt,
        @JsonProperty("recipient_name") String recipientName,
        @JsonProperty("notes") String notes,
        @JsonProperty("created_by_user_id") String createdByUserId,
        @JsonProperty("created_by_email") String createdByEmail,
        @JsonProperty("created_at") Instant createdAt) {
}
