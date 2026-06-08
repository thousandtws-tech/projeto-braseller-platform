package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record BankTransaction(
        @JsonProperty("id")          String id,
        @JsonProperty("tenant_id")   String tenantId,
        @JsonProperty("fit_id")      String fitId,
        @JsonProperty("tran_type")   String tranType,
        @JsonProperty("amount")      BigDecimal amount,
        @JsonProperty("posted_date") LocalDate postedDate,
        @JsonProperty("description") String description,
        @JsonProperty("category")    BankTransactionCategory category,
        @JsonProperty("imported_at") LocalDateTime importedAt) {
}
