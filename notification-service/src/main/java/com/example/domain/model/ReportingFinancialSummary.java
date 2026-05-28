package com.example.domain.model;

import java.math.BigDecimal;

public record ReportingFinancialSummary(
        BigDecimal grossValue,
        long entryCount) {
    public static ReportingFinancialSummary empty() {
        return new ReportingFinancialSummary(BigDecimal.ZERO, 0);
    }
}
