package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentReleaseAlert(
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("platform") String platform,
        @JsonProperty("payment_id") String paymentId,
        @JsonProperty("amount") BigDecimal amount,
        @JsonProperty("release_date") LocalDate releaseDate) {
}
