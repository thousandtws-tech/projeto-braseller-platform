package com.example.application.port.out;

import com.example.domain.model.PaymentReleaseAlert;
import com.example.domain.model.ReportingFinancialSummary;

import java.time.LocalDate;
import java.util.List;

public interface ReportingDataProvider {
    ReportingFinancialSummary summary(String tenantId, LocalDate from, LocalDate to);

    List<PaymentReleaseAlert> paymentReleases(String tenantId, String platform, LocalDate from, LocalDate to);
}
