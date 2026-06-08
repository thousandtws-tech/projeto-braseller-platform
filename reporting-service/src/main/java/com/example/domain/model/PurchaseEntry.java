package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record PurchaseEntry(
        @JsonProperty("id")            String id,
        @JsonProperty("tenant_id")     String tenantId,
        @JsonProperty("nfe_number")    String nfeNumber,
        @JsonProperty("supplier_name") String supplierName,
        @JsonProperty("issue_date")    LocalDate issueDate,
        @JsonProperty("total_cost")    BigDecimal totalCost,
        @JsonProperty("items")         List<PurchaseEntryItem> items,
        @JsonProperty("created_at")    LocalDateTime createdAt) {
    public PurchaseEntry {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
