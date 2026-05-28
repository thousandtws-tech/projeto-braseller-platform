package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record MonthlyEvolutionPoint(
        @JsonProperty("period") String period,
        @JsonProperty("gross_value") BigDecimal grossValue,
        @JsonProperty("received_value") BigDecimal receivedValue,
        @JsonProperty("fee_value") BigDecimal feeValue,
        @JsonProperty("receivable_value") BigDecimal receivableValue,
        @JsonProperty("entry_count") long entryCount) {
}
