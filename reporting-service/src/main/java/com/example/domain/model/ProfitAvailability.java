package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.util.List;

public record ProfitAvailability(
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("total_released_profit") BigDecimal totalReleasedProfit,
        @JsonProperty("total_distributed_profit") BigDecimal totalDistributedProfit,
        @JsonProperty("available_profit") BigDecimal availableProfit,
        @JsonProperty("periods") List<ProfitPeriodBalance> periods) {
    public ProfitAvailability {
        periods = periods == null ? List.of() : List.copyOf(periods);
    }
}
