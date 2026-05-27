package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;

public record InvoiceInfo(
        @JsonProperty("invoice_number") String invoiceNumber,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("issued_at") LocalDate issuedAt,
        @JsonProperty("status") String status,
        @JsonProperty("access_key") String accessKey) {
}
