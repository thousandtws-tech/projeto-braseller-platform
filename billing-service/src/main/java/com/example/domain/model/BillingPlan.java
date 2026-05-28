package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record BillingPlan(
        @JsonProperty("code") BillingPlanCode code,
        @JsonProperty("name") String name,
        @JsonProperty("description") String description,
        @JsonProperty("monthly_price") BigDecimal monthlyPrice,
        @JsonProperty("currency") String currency,
        @JsonProperty("trial_days") int trialDays,
        @JsonProperty("marketplace_limit") int marketplaceLimit,
        @JsonProperty("user_limit") int userLimit,
        @JsonProperty("active") boolean active) {
}
