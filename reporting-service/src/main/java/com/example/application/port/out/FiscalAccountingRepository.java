package com.example.application.port.out;

import com.example.application.command.CreateExpenseCommand;
import com.example.application.command.CreateProfitDistributionCommand;
import com.example.application.command.SignAccountingPeriodCommand;
import com.example.application.command.UpdateExpenseCommand;
import com.example.application.command.UpsertFiscalProfileCommand;
import com.example.domain.model.AccountingPeriodClosing;
import com.example.domain.model.ExpenseCategoryTotal;
import com.example.domain.model.ExpenseEntry;
import com.example.domain.model.ExpenseFilter;
import com.example.domain.model.ExpensePage;
import com.example.domain.model.FiscalProfile;
import com.example.domain.model.ProfitAvailability;
import com.example.domain.model.ProfitDistribution;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

public interface FiscalAccountingRepository {
    FiscalProfile upsertProfile(UpsertFiscalProfileCommand command);

    Optional<FiscalProfile> findProfile(String tenantId);

    ExpenseEntry createExpense(CreateExpenseCommand command);

    ExpenseEntry updateExpense(UpdateExpenseCommand command);

    Optional<ExpenseEntry> findExpense(String tenantId, String expenseId);

    ExpensePage searchExpenses(String tenantId, ExpenseFilter filter);

    BigDecimal sumExpenses(String tenantId, ExpenseFilter filter);

    long countExpenses(String tenantId, ExpenseFilter filter);

    List<ExpenseCategoryTotal> sumExpensesByCategory(String tenantId, ExpenseFilter filter);

    boolean deleteExpense(String tenantId, String expenseId);

    AccountingPeriodClosing signClosing(SignAccountingPeriodCommand command);

    Optional<AccountingPeriodClosing> findClosing(String tenantId, YearMonth periodMonth);

    boolean isPeriodClosed(String tenantId, LocalDate date);

    ProfitAvailability profitAvailability(String tenantId);

    List<ProfitDistribution> listProfitDistributions(String tenantId, YearMonth periodMonth);

    ProfitDistribution createProfitDistribution(CreateProfitDistributionCommand command);
}
