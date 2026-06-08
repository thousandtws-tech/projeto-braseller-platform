package com.example.application.service;

import com.example.application.command.UpsertReportEntryCommand;
import com.example.application.exception.AccountingPeriodClosedException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.FiscalAccountingRepository;
import com.example.application.port.out.ReportEntryRepository;
import com.example.domain.model.AvailableFilters;
import com.example.domain.model.DashboardView;
import com.example.domain.model.FinancialSummary;
import com.example.domain.model.MonthlyEvolutionPoint;
import com.example.domain.model.PaymentReleaseAlert;
import com.example.domain.model.PlatformComparisonPoint;
import com.example.domain.model.ReportEntry;
import com.example.domain.model.ReportEntryPage;
import com.example.domain.model.ReportEntryStatus;
import com.example.domain.model.ReportFilter;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class ReportingService {
    private static final BigDecimal ZERO = new BigDecimal("0.00");

    private final ReportEntryRepository repository;
    private final FiscalAccountingRepository fiscalAccountingRepository;

    @Inject
    public ReportingService(ReportEntryRepository repository, FiscalAccountingRepository fiscalAccountingRepository) {
        this.repository = repository;
        this.fiscalAccountingRepository = fiscalAccountingRepository;
    }

    public ReportEntry upsert(UpsertReportEntryCommand command) {
        UpsertReportEntryCommand normalized = normalize(command);
        if (fiscalAccountingRepository.isPeriodClosed(normalized.tenantId(), normalized.saleDate())) {
            throw new AccountingPeriodClosedException("accounting_period_closed");
        }
        return repository.upsert(normalized);
    }

    public DashboardView dashboard(String tenantId, ReportFilter filter) {
        return new DashboardView(
                summary(tenantId, filter),
                entries(tenantId, filter),
                monthlyEvolution(tenantId, filter),
                platformComparison(tenantId, filter),
                filters(tenantId)
        );
    }

    public FinancialSummary summary(String tenantId, ReportFilter filter) {
        return repository.summarize(requireText(tenantId, "tenantId"), filter);
    }

    public ReportEntryPage entries(String tenantId, ReportFilter filter) {
        return repository.search(requireText(tenantId, "tenantId"), filter);
    }

    public List<MonthlyEvolutionPoint> monthlyEvolution(String tenantId, ReportFilter filter) {
        return repository.monthlyEvolution(requireText(tenantId, "tenantId"), filter);
    }

    public List<PlatformComparisonPoint> platformComparison(String tenantId, ReportFilter filter) {
        return repository.platformComparison(requireText(tenantId, "tenantId"), filter);
    }

    public List<PaymentReleaseAlert> paymentReleases(String tenantId, String platform, LocalDate from, LocalDate to) {
        return repository.paymentReleases(requireText(tenantId, "tenantId"), blankToNull(platform), from, to);
    }

    public AvailableFilters filters(String tenantId) {
        return repository.availableFilters(requireText(tenantId, "tenantId"));
    }

    private UpsertReportEntryCommand normalize(UpsertReportEntryCommand command) {
        if (command == null) {
            throw new ValidationException("report entry is required");
        }

        String tenantId = requireText(command.tenantId(), "tenantId");
        String platform = requireText(command.platform(), "platform").toLowerCase();
        String orderId = requireText(command.orderId(), "orderId");
        LocalDate saleDate = command.saleDate() == null ? LocalDate.now() : command.saleDate();
        BigDecimal grossValue = money(command.grossValue());
        BigDecimal feeValue = money(command.feeValue());
        ReportEntryStatus status = command.status() == null ? ReportEntryStatus.PAID : command.status();
        if (isFinancialReversal(status)) {
            return new UpsertReportEntryCommand(
                    tenantId,
                    platform,
                    orderId,
                    saleDate,
                    ZERO,
                    ZERO,
                    ZERO,
                    ZERO,
                    command.paymentMethod() == null ? com.example.domain.model.PaymentMethod.OTHER : command.paymentMethod(),
                    status,
                    null,
                    blankToNull(command.buyerName()),
                    blankToNull(command.invoiceNumber())
            );
        }
        BigDecimal receivedValue = command.receivedValue() == null
                ? defaultReceivedValue(grossValue, feeValue, status)
                : money(command.receivedValue());
        BigDecimal receivableValue = command.receivableValue() == null
                ? defaultReceivableValue(grossValue, feeValue, status)
                : money(command.receivableValue());

        return new UpsertReportEntryCommand(
                tenantId,
                platform,
                orderId,
                saleDate,
                grossValue,
                receivedValue,
                feeValue,
                receivableValue,
                command.paymentMethod() == null ? com.example.domain.model.PaymentMethod.OTHER : command.paymentMethod(),
                status,
                command.releaseDate(),
                blankToNull(command.buyerName()),
                blankToNull(command.invoiceNumber())
        );
    }

    private boolean isFinancialReversal(ReportEntryStatus status) {
        return status == ReportEntryStatus.CANCELLED || status == ReportEntryStatus.REFUNDED;
    }

    private BigDecimal defaultReceivedValue(BigDecimal grossValue, BigDecimal feeValue, ReportEntryStatus status) {
        if (status == ReportEntryStatus.RECEIVED) {
            return grossValue.subtract(feeValue).max(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    private BigDecimal defaultReceivableValue(BigDecimal grossValue, BigDecimal feeValue, ReportEntryStatus status) {
        if (status == ReportEntryStatus.PAID || status == ReportEntryStatus.PENDING_RELEASE) {
            return grossValue.subtract(feeValue).max(BigDecimal.ZERO);
        }
        return BigDecimal.ZERO;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
