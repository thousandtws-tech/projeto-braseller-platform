package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record StandardOrder(
        @JsonProperty("order_id") String orderId,
        @JsonProperty("platform") String platform,
        @JsonProperty("date") LocalDate date,
        @JsonProperty("gross_value") BigDecimal grossValue,
        @JsonProperty("platform_fee") BigDecimal platformFee,
        @JsonProperty("net_value") BigDecimal netValue,
        @JsonProperty("payment_method") PaymentMethod paymentMethod,
        @JsonProperty("payment_date") LocalDate paymentDate,
        @JsonProperty("release_date") LocalDate releaseDate,
        @JsonProperty("status") OrderStatus status,
        @JsonProperty("buyer_name") String buyerName,
        @JsonProperty("items") List<StandardOrderItem> items,
        @JsonProperty("invoice_number") String invoiceNumber) {

    public StandardOrder {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
