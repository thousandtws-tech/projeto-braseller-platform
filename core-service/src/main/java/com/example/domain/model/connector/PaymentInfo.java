package com.example.domain.model.connector;

import com.example.domain.enums.PaymentMethod;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentInfo(
        @JsonProperty("payment_id") String paymentId,
        @JsonProperty("order_id") String orderId,
        @JsonProperty("payment_method") PaymentMethod paymentMethod,
        @JsonProperty("gross_value") BigDecimal grossValue,
        @JsonProperty("net_value") BigDecimal netValue,
        @JsonProperty("payment_date") LocalDate paymentDate,
        @JsonProperty("release_date") LocalDate releaseDate,
        @JsonProperty("status") String status) {
}
