package com.example.infrastructure.client;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ReportEntryIngestRequest(
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("platform") String platform,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("sale_date") LocalDate saleDate,
        @JsonProperty("gross_value") BigDecimal grossValue,
        @JsonProperty("received_value") BigDecimal receivedValue,
        @JsonProperty("fee_value") BigDecimal feeValue,
        @JsonProperty("receivable_value") BigDecimal receivableValue,
        @JsonProperty("payment_method") String paymentMethod,
        @JsonProperty("status") String status,
        @JsonProperty("release_date") LocalDate releaseDate,
        @JsonProperty("buyer_name") String buyerName,
        @JsonProperty("items") List<Item> items,
        @JsonProperty("invoice_number") String invoiceNumber) {

    public ReportEntryIngestRequest {
        items = items == null ? List.of() : List.copyOf(items);
    }

    public record Item(
            @JsonProperty("sku") String sku,
            @JsonProperty("title") String title,
            @JsonProperty("quantity") BigDecimal quantity,
            @JsonProperty("unit_value") BigDecimal unitValue,
            @JsonProperty("gross_value") BigDecimal grossValue) {
    }
}
