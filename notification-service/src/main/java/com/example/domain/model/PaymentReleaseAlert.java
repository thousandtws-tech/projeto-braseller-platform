package com.example.domain.model;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PaymentReleaseAlert(
        String tenantId,
        String platform,
        String paymentId,
        BigDecimal amount,
        LocalDate releaseDate) {
}
