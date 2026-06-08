package com.example.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;

public record CreateProfitDistributionCommand(
        String tenantId,
        YearMonth periodMonth,
        BigDecimal amount,
        LocalDate distributedAt,
        String recipientName,
        String notes,
        String createdByUserId,
        String createdByEmail) {
}
