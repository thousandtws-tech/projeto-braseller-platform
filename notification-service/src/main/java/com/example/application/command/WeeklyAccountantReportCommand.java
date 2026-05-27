package com.example.application.command;

import java.math.BigDecimal;
import java.time.LocalDate;

public record WeeklyAccountantReportCommand(
        String tenantId,
        String accountantEmail,
        LocalDate weekStart,
        LocalDate weekEnd,
        int totalSales,
        BigDecimal grossRevenue) {
}
