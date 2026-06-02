package com.example.application.service;

import com.example.application.command.CreateExpenseCommand;
import com.example.application.command.SignAccountingPeriodCommand;
import com.example.application.command.UpdateExpenseCommand;
import com.example.application.command.UpsertFiscalProfileCommand;
import com.example.application.exception.AccountingPeriodClosedException;
import com.example.application.exception.NotFoundException;
import com.example.application.exception.ValidationException;
import com.example.application.event.DreCalculationRequestedEvent;
import com.example.application.port.out.DreCalculationEventPublisher;
import com.example.application.port.out.DreCalculationJobRepository;
import com.example.application.port.out.FiscalAccountingRepository;
import com.example.domain.model.AccountingPeriodClosing;
import com.example.domain.model.DreCalculationJob;
import com.example.domain.model.DreStatement;
import com.example.domain.model.ExpenseAttachment;
import com.example.domain.model.ExpenseCategory;
import com.example.domain.model.ExpenseCategoryTotal;
import com.example.domain.model.ExpenseEntry;
import com.example.domain.model.ExpenseFilter;
import com.example.domain.model.ExpensePage;
import com.example.domain.model.FinancialSummary;
import com.example.domain.model.FiscalProfile;
import com.example.domain.model.ReportFilter;
import com.example.domain.model.TaxRegime;
import com.example.domain.model.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;

@ApplicationScoped
public class FiscalAccountingService {
    private static final BigDecimal ZERO = BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    private static final BigDecimal ONE = BigDecimal.ONE;

    private final FiscalAccountingRepository fiscalRepository;
    private final ReportingService reportingService;
    private final DreCalculationJobRepository dreCalculationJobRepository;
    private final DreCalculationEventPublisher dreCalculationEventPublisher;

    @Inject
    public FiscalAccountingService(
            FiscalAccountingRepository fiscalRepository,
            ReportingService reportingService,
            DreCalculationJobRepository dreCalculationJobRepository,
            DreCalculationEventPublisher dreCalculationEventPublisher) {
        this.fiscalRepository = fiscalRepository;
        this.reportingService = reportingService;
        this.dreCalculationJobRepository = dreCalculationJobRepository;
        this.dreCalculationEventPublisher = dreCalculationEventPublisher;
    }

    public FiscalProfile upsertProfile(UpsertFiscalProfileCommand command) {
        if (command == null) {
            throw new ValidationException("fiscal profile is required");
        }
        String tenantId = requireText(command.tenantId(), "tenantId");
        TaxRegime taxRegime = command.taxRegime();
        if (taxRegime == null) {
            throw new ValidationException("taxRegime is required");
        }
        BigDecimal taxRate = rate(command.estimatedTaxRate());
        return fiscalRepository.upsertProfile(new UpsertFiscalProfileCommand(
                tenantId,
                taxRegime,
                taxRate,
                trimToNull(command.notes())
        ));
    }

    public FiscalProfile profile(String tenantId) {
        return fiscalRepository.findProfile(requireText(tenantId, "tenantId"))
                .orElseThrow(() -> new NotFoundException("fiscal_profile_not_found"));
    }

    public ExpenseEntry createExpense(CreateExpenseCommand command) {
        CreateExpenseCommand normalized = normalize(command);
        requireOpenPeriod(normalized.tenantId(), normalized.expenseDate());
        return fiscalRepository.createExpense(normalized);
    }

    public ExpenseEntry updateExpense(UpdateExpenseCommand command) {
        if (command == null) {
            throw new ValidationException("expense is required");
        }
        String tenantId = requireText(command.tenantId(), "tenantId");
        String expenseId = requireText(command.expenseId(), "expenseId");
        ExpenseEntry existing = expense(tenantId, expenseId);
        requireOpenPeriod(tenantId, existing.expenseDate());
        LocalDate newExpenseDate = command.expenseDate() == null ? LocalDate.now() : command.expenseDate();
        requireOpenPeriod(tenantId, newExpenseDate);
        UpdateExpenseCommand normalized = new UpdateExpenseCommand(
                tenantId,
                expenseId,
                newExpenseDate,
                command.category() == null ? ExpenseCategory.OTHER : command.category(),
                requireText(command.description(), "description"),
                moneyPositive(command.amount()),
                normalizeAttachment(command.attachment())
        );
        return fiscalRepository.updateExpense(normalized);
    }

    public ExpenseEntry expense(String tenantId, String expenseId) {
        return fiscalRepository.findExpense(requireText(tenantId, "tenantId"), requireText(expenseId, "expenseId"))
                .orElseThrow(() -> new NotFoundException("expense_not_found"));
    }

    public ExpensePage expenses(String tenantId, ExpenseFilter filter) {
        return fiscalRepository.searchExpenses(requireText(tenantId, "tenantId"), safeFilter(filter));
    }

    public void deleteExpense(String tenantId, String expenseId) {
        String resolvedTenantId = requireText(tenantId, "tenantId");
        String resolvedExpenseId = requireText(expenseId, "expenseId");
        ExpenseEntry existing = expense(resolvedTenantId, resolvedExpenseId);
        requireOpenPeriod(resolvedTenantId, existing.expenseDate());
        boolean deleted = fiscalRepository.deleteExpense(resolvedTenantId, resolvedExpenseId);
        if (!deleted) {
            throw new NotFoundException("expense_not_found");
        }
    }

    public AccountingPeriodClosing signClosing(SignAccountingPeriodCommand command) {
        if (command == null) {
            throw new ValidationException("closing signature is required");
        }
        String tenantId = requireText(command.tenantId(), "tenantId");
        YearMonth periodMonth = command.periodMonth();
        if (periodMonth == null) {
            throw new ValidationException("periodMonth is required");
        }
        return fiscalRepository.signClosing(new SignAccountingPeriodCommand(
                tenantId,
                periodMonth,
                requireText(command.signedByUserId(), "signedByUserId"),
                requireText(command.signedByEmail(), "signedByEmail"),
                requireText(command.signatureHash(), "signatureHash")
        ));
    }

    public AccountingPeriodClosing signClosing(String tenantId, YearMonth periodMonth, TenantContext signer, String signatureHash) {
        if (signer == null) {
            throw new ValidationException("signer is required");
        }
        return signClosing(new SignAccountingPeriodCommand(
                tenantId,
                periodMonth,
                signer.userId(),
                signer.email(),
                signatureHash
        ));
    }

    public AccountingPeriodClosing closing(String tenantId, YearMonth periodMonth) {
        return fiscalRepository.findClosing(requireText(tenantId, "tenantId"), requirePeriodMonth(periodMonth))
                .orElseThrow(() -> new NotFoundException("accounting_period_closing_not_found"));
    }

    public DreStatement dre(String tenantId, LocalDate from, LocalDate to) {
        String resolvedTenantId = requireText(tenantId, "tenantId");
        DrePeriod period = resolvePeriod(from, to);

        ReportFilter reportFilter = new ReportFilter(period.from(), period.to(), null, null, null, null, null, null, null, null);
        ExpenseFilter expenseFilter = new ExpenseFilter(period.from(), period.to(), null, null, null);
        FinancialSummary summary = reportingService.summary(resolvedTenantId, reportFilter);
        FiscalProfile profile = fiscalRepository.findProfile(resolvedTenantId).orElse(null);
        BigDecimal estimatedTaxRate = profile == null ? BigDecimal.ZERO : profile.estimatedTaxRate();
        BigDecimal grossRevenue = money(summary.grossValue());
        BigDecimal marketplaceFees = money(summary.feeValue());
        BigDecimal estimatedTaxes = grossRevenue.multiply(estimatedTaxRate).setScale(2, RoundingMode.HALF_UP);
        BigDecimal operatingExpenses = money(fiscalRepository.sumExpenses(resolvedTenantId, expenseFilter));
        long expenseCount = fiscalRepository.countExpenses(resolvedTenantId, expenseFilter);
        List<ExpenseCategoryTotal> expensesByCategory = fiscalRepository.sumExpensesByCategory(resolvedTenantId, expenseFilter);
        BigDecimal netResult = grossRevenue
                .subtract(marketplaceFees)
                .subtract(estimatedTaxes)
                .subtract(operatingExpenses)
                .setScale(2, RoundingMode.HALF_UP);

        return new DreStatement(
                resolvedTenantId,
                period.from(),
                period.to(),
                profile == null ? null : profile.taxRegime(),
                estimatedTaxRate,
                grossRevenue,
                marketplaceFees,
                estimatedTaxes,
                operatingExpenses,
                netResult,
                summary.entryCount(),
                expenseCount,
                expensesByCategory
        );
    }

    public DreCalculationJob requestDreCalculation(String tenantId, LocalDate from, LocalDate to, TenantContext requester) {
        if (requester == null) {
            throw new ValidationException("requester is required");
        }
        String resolvedTenantId = requireText(tenantId, "tenantId");
        DrePeriod period = resolvePeriod(from, to);
        DreCalculationRequestedEvent event = DreCalculationRequestedEvent.create(
                resolvedTenantId,
                period.from(),
                period.to(),
                requester.userId(),
                requester.email()
        );
        DreCalculationJob job = dreCalculationJobRepository.createQueued(event);
        dreCalculationEventPublisher.publish(event);
        return job;
    }

    public DreCalculationJob dreCalculationJob(String tenantId, String jobId) {
        return dreCalculationJobRepository.find(requireText(tenantId, "tenantId"), requireText(jobId, "jobId"))
                .orElseThrow(() -> new NotFoundException("dre_calculation_job_not_found"));
    }

    public DreCalculationJob processDreCalculation(DreCalculationRequestedEvent event) {
        if (event == null) {
            throw new ValidationException("dre calculation event is required");
        }
        if (!dreCalculationJobRepository.tryMarkProcessing(event.jobId())) {
            return null;
        }
        try {
            DreStatement statement = dre(event.tenantId(), event.from(), event.to());
            return dreCalculationJobRepository.markCompleted(event.jobId(), statement);
        } catch (RuntimeException exception) {
            dreCalculationJobRepository.markFailed(event.jobId(), exception.getMessage());
            throw exception;
        }
    }

    private CreateExpenseCommand normalize(CreateExpenseCommand command) {
        if (command == null) {
            throw new ValidationException("expense is required");
        }
        return new CreateExpenseCommand(
                requireText(command.tenantId(), "tenantId"),
                command.expenseDate() == null ? LocalDate.now() : command.expenseDate(),
                command.category() == null ? ExpenseCategory.OTHER : command.category(),
                requireText(command.description(), "description"),
                moneyPositive(command.amount()),
                normalizeAttachment(command.attachment())
        );
    }

    private ExpenseFilter safeFilter(ExpenseFilter filter) {
        if (filter == null) {
            return new ExpenseFilter(null, null, null, null, null);
        }
        if (filter.from() != null && filter.to() != null && filter.from().isAfter(filter.to())) {
            throw new ValidationException("invalid_period");
        }
        return filter;
    }

    private DrePeriod resolvePeriod(LocalDate from, LocalDate to) {
        LocalDate resolvedTo = to == null ? LocalDate.now() : to;
        LocalDate resolvedFrom = from == null ? resolvedTo.withDayOfMonth(1) : from;
        if (resolvedFrom.isAfter(resolvedTo)) {
            throw new ValidationException("invalid_period");
        }
        return new DrePeriod(resolvedFrom, resolvedTo);
    }

    private ExpenseAttachment normalizeAttachment(ExpenseAttachment attachment) {
        if (attachment == null) {
            throw new ValidationException("expense_attachment_required");
        }
        String publicId = trimToNull(attachment.publicId());
        String secureUrl = trimToNull(attachment.secureUrl());
        String resourceType = trimToNull(attachment.resourceType());
        String originalFilename = trimToNull(attachment.originalFilename());
        String contentType = trimToNull(attachment.contentType());
        Long sizeBytes = attachment.sizeBytes();
        boolean empty = publicId == null
                && secureUrl == null
                && resourceType == null
                && originalFilename == null
                && contentType == null
                && sizeBytes == null;
        if (empty) {
            throw new ValidationException("expense_attachment_required");
        }
        if (publicId == null) {
            throw new ValidationException("attachment_public_id_required");
        }
        if (secureUrl == null || !(secureUrl.startsWith("https://") || secureUrl.startsWith("http://"))) {
            throw new ValidationException("attachment_secure_url_invalid");
        }
        if (sizeBytes != null && sizeBytes < 0) {
            throw new ValidationException("attachment_size_invalid");
        }
        return new ExpenseAttachment(
                publicId,
                secureUrl,
                resourceType == null ? "image" : resourceType,
                originalFilename,
                contentType,
                sizeBytes
        );
    }

    private BigDecimal rate(BigDecimal value) {
        BigDecimal rate = value == null ? BigDecimal.ZERO : value;
        if (rate.compareTo(BigDecimal.ZERO) < 0 || rate.compareTo(ONE) > 0) {
            throw new ValidationException("estimated_tax_rate_must_be_between_0_and_1");
        }
        return rate.setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal moneyPositive(BigDecimal value) {
        BigDecimal amount = value == null ? BigDecimal.ZERO : value;
        if (amount.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("amount_must_be_positive");
        }
        return amount.setScale(2, RoundingMode.HALF_UP);
    }

    private void requireOpenPeriod(String tenantId, LocalDate date) {
        if (fiscalRepository.isPeriodClosed(tenantId, date)) {
            throw new AccountingPeriodClosedException("accounting_period_closed");
        }
    }

    private YearMonth requirePeriodMonth(YearMonth value) {
        if (value == null) {
            throw new ValidationException("periodMonth is required");
        }
        return value;
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? ZERO : value.setScale(2, RoundingMode.HALF_UP);
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private record DrePeriod(LocalDate from, LocalDate to) {
    }
}
