package com.example.application.port.out;

import com.example.application.command.UpsertReportEntryCommand;
import com.example.domain.model.AvailableFilters;
import com.example.domain.model.FinancialSummary;
import com.example.domain.model.MonthlyEvolutionPoint;
import com.example.domain.model.PaymentReleaseAlert;
import com.example.domain.model.PlatformComparisonPoint;
import com.example.domain.model.ReportEntry;
import com.example.domain.model.ReportEntryPage;
import com.example.domain.model.ReportFilter;

import java.math.BigDecimal;
import java.util.List;
import java.time.LocalDate;

public interface ReportEntryRepository {
    ReportEntry upsert(UpsertReportEntryCommand command);

    FinancialSummary summarize(String tenantId, ReportFilter filter);

    ReportEntryPage search(String tenantId, ReportFilter filter);

    List<ReportEntry> listForExport(String tenantId, ReportFilter filter);

    List<PaymentReleaseAlert> paymentReleases(String tenantId, String platform, LocalDate from, LocalDate to);

    List<MonthlyEvolutionPoint> monthlyEvolution(String tenantId, ReportFilter filter);

    List<PlatformComparisonPoint> platformComparison(String tenantId, ReportFilter filter);

    AvailableFilters availableFilters(String tenantId);

    BigDecimal outstandingReceivables(String tenantId, LocalDate asOf);
}
