package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record StockItem(
        @JsonProperty("id")          String id,
        @JsonProperty("tenant_id")   String tenantId,
        @JsonProperty("sku")         String sku,
        @JsonProperty("description") String description,
        @JsonProperty("unit_cost")   BigDecimal unitCost,
        @JsonProperty("quantity")    BigDecimal quantity,
        @JsonProperty("created_at")  LocalDateTime createdAt,
        @JsonProperty("updated_at")  LocalDateTime updatedAt) {
}
