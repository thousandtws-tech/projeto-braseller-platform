package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record FeeInfo(
        @JsonProperty("order_id") String orderId,
        @JsonProperty("type") String type,
        @JsonProperty("description") String description,
        @JsonProperty("amount") BigDecimal amount) {
}
