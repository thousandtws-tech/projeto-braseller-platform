package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ExpenseCategoryTotal(
        @JsonProperty("category") ExpenseCategory category,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("entry_count") long entryCount) {
}
