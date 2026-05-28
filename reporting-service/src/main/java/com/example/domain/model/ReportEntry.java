package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;

public record ReportEntry(
        @JsonProperty("id") String id,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("platform") String platform,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("sale_date") LocalDate saleDate,
        @JsonProperty("gross_value") BigDecimal grossValue,
        @JsonProperty("received_value") BigDecimal receivedValue,
        @JsonProperty("fee_value") BigDecimal feeValue,
        @JsonProperty("receivable_value") BigDecimal receivableValue,
        @JsonProperty("payment_method") PaymentMethod paymentMethod,
        @JsonProperty("status") ReportEntryStatus status,
        @JsonProperty("release_date") LocalDate releaseDate,
        @JsonProperty("buyer_name") String buyerName,
        @JsonProperty("invoice_number") String invoiceNumber,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {
}
