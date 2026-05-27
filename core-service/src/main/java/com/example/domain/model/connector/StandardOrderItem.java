package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record StandardOrderItem(
        @JsonProperty("sku") String sku,
        @JsonProperty("title") String title,
        @JsonProperty("quantity") int quantity,
        @JsonProperty("unit_value") BigDecimal unitValue,
        @JsonProperty("gross_value") BigDecimal grossValue) {
}
