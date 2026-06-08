package com.example.application.command;

import java.math.BigDecimal;
import java.time.YearMonth;

public record SignAccountingPeriodCommand(
        String tenantId,
        YearMonth periodMonth,
        String signedByUserId,
        String signedByEmail,
        String signatureHash,
        BigDecimal distributableProfit) {
}
