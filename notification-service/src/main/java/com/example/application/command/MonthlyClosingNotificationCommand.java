package com.example.application.command;

import java.math.BigDecimal;
import java.time.YearMonth;

public record MonthlyClosingNotificationCommand(
        String tenantId,
        String recipientEmail,
        YearMonth period,
        int totalSales,
        BigDecimal grossRevenue) {
}
