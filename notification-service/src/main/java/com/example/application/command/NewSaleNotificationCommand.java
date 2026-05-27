package com.example.application.command;

import java.math.BigDecimal;

public record NewSaleNotificationCommand(
        String tenantId,
        String recipientEmail,
        String marketplace,
        String orderId,
        BigDecimal amount) {
}
