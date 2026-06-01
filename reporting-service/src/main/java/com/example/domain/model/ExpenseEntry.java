package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ExpenseEntry(
        @JsonProperty("id") String id,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("expense_date") LocalDate expenseDate,
        @JsonProperty("category") ExpenseCategory category,
        @JsonProperty("description") String description,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("attachment") ExpenseAttachment attachment,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {
}
