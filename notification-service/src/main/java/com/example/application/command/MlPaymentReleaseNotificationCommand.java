package com.example.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record MlPaymentReleaseNotificationCommand(
        String tenantId,
        String recipientEmail,
        String paymentId,
        BigDecimal amount,
        LocalDate releaseDate) {
}
