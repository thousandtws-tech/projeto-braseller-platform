package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;

public record FiscalProfile(
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("tax_regime") TaxRegime taxRegime,
        @JsonProperty("estimated_tax_rate") BigDecimal estimatedTaxRate,
        @JsonProperty("notes") String notes,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {
}
