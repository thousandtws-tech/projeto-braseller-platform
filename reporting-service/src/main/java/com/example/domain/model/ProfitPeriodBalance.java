package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record ProfitPeriodBalance(
        @JsonProperty("period_month") String periodMonth,
        @JsonProperty("signed_at") Instant signedAt,
        @JsonProperty("distributable_profit") BigDecimal distributableProfit,
        @JsonProperty("distributed_profit") BigDecimal distributedProfit,
        @JsonProperty("available_profit") BigDecimal availableProfit) {
}
