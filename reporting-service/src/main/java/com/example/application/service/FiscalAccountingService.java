package com.example.application.service;

import com.example.application.command.CreateExpenseCommand;
import com.example.application.command.CreateProfitDistributionCommand;
import com.example.application.command.SignAccountingPeriodCommand;
import com.example.application.command.UpdateExpenseCommand;
import com.example.application.command.UpsertFiscalProfileCommand;
import com.example.application.exception.AccountingPeriodClosedException;
import com.example.application.exception.NotFoundException;
import com.example.application.exception.ValidationException;
import com.example.application.event.DreCalculationRequestedEvent;
import com.example.application.port.out.BankTransactionRepository;
import com.example.application.port.out.DreCalculationEventPublisher;
import com.example.application.port.out.DreCalculationJobRepository;
import com.example.application.port.out.FiscalAccountingRepository;
import com.example.application.port.out.ReportEntryRepository;
import com.example.application.port.out.StockRepository;
import com.example.domain.model.AccountingPeriodClosing;
import com.example.domain.model.BalanceSheetStatement;
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
import com.example.domain.model.ProfitAvailability;
import com.example.domain.model.ProfitDistribution;
import com.example.domain.model.PurchaseEntry;
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
    private static final BigDecimal SIMPLES_MAX_REVENUE = new BigDecimal("4800000.00");
    private static final BigDecimal PRESUMED_IRPJ_COMMERCE_RATE = new BigDecimal("0.08");
    private static final BigDecimal PRESUMED_CSLL_COMMERCE_RATE = new BigDecimal("0.12");
    private static final BigDecimal PRESUMED_PIS_COFINS_RATE = new BigDecimal("0.0365");
    private static final BigDecimal REAL_PIS_COFINS_RATE = new BigDecimal("0.0925");
    private static final BigDecimal IRPJ_RATE = new BigDecimal("0.15");
    private static final BigDecimal IRPJ_ADDITIONAL_RATE = new BigDecimal("0.10");
    private static final BigDecimal IRPJ_ADDITIONAL_MONTHLY_THRESHOLD = new BigDecimal("20000.00");
    private static final BigDecimal CSLL_RATE = new BigDecimal("0.09");
    private static final LocalDate INCEPTION_DATE = LocalDate.of(2000, 1, 1);
    private static final List<SimplesBracket> SIMPLES_COMMERCE_BRACKETS = List.of(
            new SimplesBracket(new BigDecimal("180000.00"), new BigDecimal("0.0400"), ZERO),
            new SimplesBracket(new BigDecimal("360000.00"), new BigDecimal("0.0730"), new BigDecimal("5940.00")),
            new SimplesBracket(new BigDecimal("720000.00"), new BigDecimal("0.0950"), new BigDecimal("13860.00")),
            new SimplesBracket(new BigDecimal("1800000.00"), new BigDecimal("0.1070"), new BigDecimal("22500.00")),
            new SimplesBracket(new BigDecimal("3600000.00"), new BigDecimal("0.1430"), new BigDecimal("87300.00")),
            new SimplesBracket(SIMPLES_MAX_REVENUE, new BigDecimal("0.1900"), new BigDecimal("378000.00"))
    );

    private final FiscalAccountingRepository fiscalRepository;
    private final ReportingService reportingService;
    private final DreCalculationJobRepository dreCalculationJobRepository;
    private final DreCalculationEventPublisher dreCalculationEventPublisher;
    private final StockRepository stockRepository;
    private final BankTransactionRepository bankTransactionRepository;
    private final ReportEntryRepository reportEntryRepository;

    @Inject
    public FiscalAccountingService(
            FiscalAccountingRepository fiscalRepository,
            ReportingService reportingService,
            DreCalculationJobRepository dreCalculationJobRepository,
            DreCalculationEventPublisher dreCalculationEventPublisher,
            StockRepository stockRepository,
            BankTransactionRepository bankTransactionRepository,
            ReportEntryRepository reportEntryRepository) {
        this.fiscalRepository = fiscalRepository;
        this.reportingService = reportingService;
        this.dreCalculationJobRepository = dreCalculationJobRepository;
        this.dreCalculationEventPublisher = dreCalculationEventPublisher;
        this.stockRepository = stockRepository;
        this.bankTransactionRepository = bankTransactionRepository;
        this.reportEntryRepository = reportEntryRepository;
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
        BigDecimal distributableProfit = command.distributableProfit();
        if (distributableProfit == null) {
            // Calculate from DRE if not provided
            DreStatement dreStatement = dre(tenantId, periodMonth.atDay(1), periodMonth.atEndOfMonth());
            distributableProfit = dreStatement.distributableProfit();
        }
        return fiscalRepository.signClosing(new SignAccountingPeriodCommand(
                tenantId,
                periodMonth,
                requireText(command.signedByUserId(), "signedByUserId"),
                requireText(command.signedByEmail(), "signedByEmail"),
                requireText(command.signatureHash(), "signatureHash"),
                distributableProfit
        ));
    }

    public AccountingPeriodClosing signClosing(String tenantId, YearMonth periodMonth, TenantContext signer, String signatureHash) {
        if (signer == null) {
            throw new ValidationException("signer is required");
        }
        DreStatement dreStatement = dre(tenantId, periodMonth.atDay(1), periodMonth.atEndOfMonth());
        return fiscalRepository.signClosing(new SignAccountingPeriodCommand(
                tenantId,
                periodMonth,
                signer.userId(),
                signer.email(),
                signatureHash,
                dreStatement.distributableProfit()
        ));
    }

    public AccountingPeriodClosing closing(String tenantId, YearMonth periodMonth) {
        return fiscalRepository.findClosing(requireText(tenantId, "tenantId"), requirePeriodMonth(periodMonth))
                .orElseThrow(() -> new NotFoundException("accounting_period_closing_not_found"));
    }

    public ProfitAvailability profitAvailability(String tenantId) {
        return fiscalRepository.profitAvailability(requireText(tenantId, "tenantId"));
    }

    public List<ProfitDistribution> profitDistributions(String tenantId, YearMonth periodMonth) {
        return fiscalRepository.listProfitDistributions(requireText(tenantId, "tenantId"), periodMonth);
    }

    public ProfitDistribution createProfitDistribution(CreateProfitDistributionCommand command) {
        if (command == null) {
            throw new ValidationException("profit_distribution is required");
        }
        YearMonth periodMonth = requirePeriodMonth(command.periodMonth());
        return fiscalRepository.createProfitDistribution(new CreateProfitDistributionCommand(
                requireText(command.tenantId(), "tenantId"),
                periodMonth,
                moneyPositive(command.amount()),
                command.distributedAt() == null ? LocalDate.now() : command.distributedAt(),
                trimToNull(command.recipientName()),
                trimToNull(command.notes()),
                requireText(command.createdByUserId(), "createdByUserId"),
                requireText(command.createdByEmail(), "createdByEmail")
        ));
    }

    public DreStatement dre(String tenantId, LocalDate from, LocalDate to) {
        String resolvedTenantId = requireText(tenantId, "tenantId");
        DrePeriod period = resolvePeriod(from, to);

        ReportFilter reportFilter = new ReportFilter(period.from(), period.to(), null, null, null, null, null, null, null, null);
        ExpenseFilter expenseFilter = new ExpenseFilter(period.from(), period.to(), null, null, null);
        FinancialSummary summary = reportingService.summary(resolvedTenantId, reportFilter);
        FiscalProfile profile = fiscalRepository.findProfile(resolvedTenantId).orElse(null);
        BigDecimal grossRevenue = money(summary.grossValue());
        BigDecimal marketplaceFees = money(summary.feeValue());
        BigDecimal cmv = money(stockRepository.sumCmv(resolvedTenantId, period.from(), period.to()));
        BigDecimal operatingExpenses = money(fiscalRepository.sumExpenses(resolvedTenantId, expenseFilter));
        BigDecimal bankingExpenses = money(bankTransactionRepository.sumExpenses(resolvedTenantId, period.from(), period.to()));
        long expenseCount = fiscalRepository.countExpenses(resolvedTenantId, expenseFilter);
        List<ExpenseCategoryTotal> expensesByCategory = fiscalRepository.sumExpensesByCategory(resolvedTenantId, expenseFilter);
        BigDecimal resultBeforeTaxes = grossRevenue
                .subtract(marketplaceFees)
                .subtract(cmv)
                .subtract(operatingExpenses)
                .subtract(bankingExpenses)
                .setScale(2, RoundingMode.HALF_UP);
        TaxEstimate taxEstimate = taxEstimate(resolvedTenantId, period, profile, grossRevenue, resultBeforeTaxes);
        BigDecimal estimatedTaxRate = taxEstimate.effectiveRate();
        BigDecimal estimatedTaxes = taxEstimate.amount();
        BigDecimal netResult = resultBeforeTaxes
                .subtract(estimatedTaxes)
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal distributableProfit = netResult.compareTo(ZERO) > 0 ? netResult : ZERO;

        return new DreStatement(
                resolvedTenantId,
                period.from(),
                period.to(),
                profile == null ? null : profile.taxRegime(),
                estimatedTaxRate,
                grossRevenue,
                marketplaceFees,
                estimatedTaxes,
                cmv,
                operatingExpenses,
                bankingExpenses,
                netResult,
                distributableProfit,
                summary.entryCount(),
                expenseCount,
                expensesByCategory
        );
    }

    public BalanceSheetStatement balanceSheet(String tenantId, LocalDate asOf) {
        String resolvedTenantId = requireText(tenantId, "tenantId");
        LocalDate resolvedAsOf = asOf == null ? LocalDate.now() : asOf;

        BigDecimal cashAndBank = money(bankTransactionRepository.balanceAsOf(resolvedTenantId, resolvedAsOf));
        BigDecimal accountsReceivable = money(reportEntryRepository.outstandingReceivables(resolvedTenantId, resolvedAsOf));
        BigDecimal inventory = money(stockRepository.totalInventoryValue(resolvedTenantId));
        BigDecimal totalAssets = cashAndBank.add(accountsReceivable).add(inventory).setScale(2, RoundingMode.HALF_UP);

        LocalDate periodStart = resolvedAsOf.withDayOfMonth(1);
        BigDecimal accountsPayable = money(stockRepository.listPurchaseEntries(resolvedTenantId, periodStart, resolvedAsOf).stream()
                .map(PurchaseEntry::totalCost)
                .reduce(BigDecimal.ZERO, BigDecimal::add));
        DreStatement currentPeriodDre = dre(resolvedTenantId, periodStart, resolvedAsOf);
        BigDecimal taxesPayable = money(currentPeriodDre.estimatedTaxes());
        BigDecimal totalLiabilities = accountsPayable.add(taxesPayable).setScale(2, RoundingMode.HALF_UP);

        BigDecimal equity = totalAssets.subtract(totalLiabilities).setScale(2, RoundingMode.HALF_UP);
        BigDecimal totalLiabilitiesAndEquity = totalLiabilities.add(equity).setScale(2, RoundingMode.HALF_UP);

        DreStatement inceptionDre = dre(resolvedTenantId, INCEPTION_DATE, resolvedAsOf);
        BigDecimal accumulatedNetResult = money(inceptionDre.netResult());

        return new BalanceSheetStatement(
                resolvedTenantId,
                resolvedAsOf,
                cashAndBank,
                accountsReceivable,
                inventory,
                totalAssets,
                accountsPayable,
                taxesPayable,
                totalLiabilities,
                equity,
                accumulatedNetResult,
                totalLiabilitiesAndEquity
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
        ExpenseAttachment attachment = normalizeAttachment(command.attachment());
        if (attachment == null) {
            throw new ValidationException("expense_attachment_required");
        }
        return new CreateExpenseCommand(
                requireText(command.tenantId(), "tenantId"),
                command.expenseDate() == null ? LocalDate.now() : command.expenseDate(),
                command.category() == null ? ExpenseCategory.OTHER : command.category(),
                requireText(command.description(), "description"),
                moneyPositive(command.amount()),
                attachment
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
            return null;
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
            return null;
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
        if (rate.compareTo(ONE) > 0 && rate.compareTo(new BigDecimal("100.00")) <= 0) {
            rate = rate.divide(new BigDecimal("100.00"), 6, RoundingMode.HALF_UP);
        }
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

    private TaxEstimate taxEstimate(String tenantId, DrePeriod period, FiscalProfile profile, BigDecimal grossRevenue, BigDecimal resultBeforeTaxes) {
        if (grossRevenue.compareTo(ZERO) <= 0) {
            return new TaxEstimate(ZERO, fallbackRate(profile));
        }
        if (profile == null || profile.taxRegime() == null) {
            BigDecimal rate = fallbackRate(profile);
            return new TaxEstimate(money(grossRevenue.multiply(rate)), rate);
        }
        return switch (profile.taxRegime()) {
            case SIMPLES_NACIONAL -> simplesTaxEstimate(tenantId, period, profile, grossRevenue);
            case LUCRO_PRESUMIDO -> lucroPresumidoTaxEstimate(period, profile, grossRevenue);
            case LUCRO_REAL -> lucroRealTaxEstimate(period, profile, grossRevenue, resultBeforeTaxes);
        };
    }

    private TaxEstimate simplesTaxEstimate(String tenantId, DrePeriod period, FiscalProfile profile, BigDecimal grossRevenue) {
        BigDecimal rbt12 = rollingGrossRevenue(tenantId, period);
        BigDecimal referenceRevenue = rbt12.compareTo(ZERO) > 0 ? rbt12 : grossRevenue;
        if (referenceRevenue.compareTo(ZERO) <= 0) {
            BigDecimal rate = fallbackRate(profile);
            return new TaxEstimate(money(grossRevenue.multiply(rate)), rate);
        }
        SimplesBracket bracket = simplesBracket(referenceRevenue);
        BigDecimal effectiveRate = referenceRevenue.multiply(bracket.nominalRate())
                .subtract(bracket.deduction())
                .divide(referenceRevenue, 6, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(ONE)
                .setScale(4, RoundingMode.HALF_UP);
        return new TaxEstimate(money(grossRevenue.multiply(effectiveRate)), effectiveRate);
    }

    private TaxEstimate lucroPresumidoTaxEstimate(DrePeriod period, FiscalProfile profile, BigDecimal grossRevenue) {
        int months = monthsInPeriod(period);
        BigDecimal irpjBase = grossRevenue.multiply(PRESUMED_IRPJ_COMMERCE_RATE);
        BigDecimal csllBase = grossRevenue.multiply(PRESUMED_CSLL_COMMERCE_RATE);
        BigDecimal taxes = grossRevenue.multiply(PRESUMED_PIS_COFINS_RATE)
                .add(irpjBase.multiply(IRPJ_RATE))
                .add(irpjAdditional(irpjBase, months))
                .add(csllBase.multiply(CSLL_RATE));
        BigDecimal amount = money(taxes);
        return new TaxEstimate(amount, effectiveRate(grossRevenue, amount, fallbackRate(profile)));
    }

    private TaxEstimate lucroRealTaxEstimate(DrePeriod period, FiscalProfile profile, BigDecimal grossRevenue, BigDecimal resultBeforeTaxes) {
        int months = monthsInPeriod(period);
        BigDecimal taxableProfit = resultBeforeTaxes.max(BigDecimal.ZERO);
        BigDecimal taxes = grossRevenue.multiply(REAL_PIS_COFINS_RATE)
                .add(taxableProfit.multiply(IRPJ_RATE))
                .add(irpjAdditional(taxableProfit, months))
                .add(taxableProfit.multiply(CSLL_RATE));
        BigDecimal amount = money(taxes);
        return new TaxEstimate(amount, effectiveRate(grossRevenue, amount, fallbackRate(profile)));
    }

    private BigDecimal rollingGrossRevenue(String tenantId, DrePeriod period) {
        LocalDate to = period.from().minusDays(1);
        LocalDate from = period.from().minusMonths(12);
        if (to.isBefore(from)) {
            return ZERO;
        }
        ReportFilter filter = new ReportFilter(from, to, null, null, null, null, null, null, null, null);
        return money(reportingService.summary(tenantId, filter).grossValue());
    }

    private SimplesBracket simplesBracket(BigDecimal referenceRevenue) {
        for (SimplesBracket bracket : SIMPLES_COMMERCE_BRACKETS) {
            if (referenceRevenue.compareTo(bracket.limit()) <= 0) {
                return bracket;
            }
        }
        return SIMPLES_COMMERCE_BRACKETS.get(SIMPLES_COMMERCE_BRACKETS.size() - 1);
    }

    private BigDecimal irpjAdditional(BigDecimal base, int months) {
        BigDecimal threshold = IRPJ_ADDITIONAL_MONTHLY_THRESHOLD.multiply(BigDecimal.valueOf(Math.max(1, months)));
        BigDecimal excess = base.subtract(threshold);
        if (excess.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }
        return excess.multiply(IRPJ_ADDITIONAL_RATE);
    }

    private int monthsInPeriod(DrePeriod period) {
        YearMonth from = YearMonth.from(period.from());
        YearMonth to = YearMonth.from(period.to());
        return Math.max(1, (to.getYear() - from.getYear()) * 12 + to.getMonthValue() - from.getMonthValue() + 1);
    }

    private BigDecimal fallbackRate(FiscalProfile profile) {
        return profile == null || profile.estimatedTaxRate() == null
                ? BigDecimal.ZERO.setScale(4, RoundingMode.HALF_UP)
                : profile.estimatedTaxRate().setScale(4, RoundingMode.HALF_UP);
    }

    private BigDecimal effectiveRate(BigDecimal grossRevenue, BigDecimal taxes, BigDecimal fallbackRate) {
        if (grossRevenue == null || grossRevenue.compareTo(ZERO) <= 0) {
            return fallbackRate;
        }
        return taxes.divide(grossRevenue, 6, RoundingMode.HALF_UP)
                .max(BigDecimal.ZERO)
                .min(ONE)
                .setScale(4, RoundingMode.HALF_UP);
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

    private record TaxEstimate(BigDecimal amount, BigDecimal effectiveRate) {
    }

    private record SimplesBracket(BigDecimal limit, BigDecimal nominalRate, BigDecimal deduction) {
    }
}
