package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record PurchaseEntryItem(
        @JsonProperty("id")                String id,
        @JsonProperty("purchase_entry_id") String purchaseEntryId,
        @JsonProperty("sku")               String sku,
        @JsonProperty("description")       String description,
        @JsonProperty("quantity")          BigDecimal quantity,
        @JsonProperty("unit_cost")         BigDecimal unitCost,
        @JsonProperty("total_cost")        BigDecimal totalCost) {
}
