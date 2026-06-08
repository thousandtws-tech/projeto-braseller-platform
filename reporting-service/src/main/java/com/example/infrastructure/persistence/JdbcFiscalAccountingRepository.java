package com.example.infrastructure.persistence;

import com.example.application.command.CreateExpenseCommand;
import com.example.application.command.CreateProfitDistributionCommand;
import com.example.application.command.SignAccountingPeriodCommand;
import com.example.application.command.UpdateExpenseCommand;
import com.example.application.command.UpsertFiscalProfileCommand;
import com.example.application.exception.AccountingPeriodClosedException;
import com.example.application.exception.NotFoundException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.FiscalAccountingRepository;
import com.example.domain.model.AccountingPeriodClosing;
import com.example.domain.model.ExpenseAttachment;
import com.example.domain.model.ExpenseCategory;
import com.example.domain.model.ExpenseCategoryTotal;
import com.example.domain.model.ExpenseEntry;
import com.example.domain.model.ExpenseFilter;
import com.example.domain.model.ExpensePage;
import com.example.domain.model.FiscalProfile;
import com.example.domain.model.ProfitAvailability;
import com.example.domain.model.ProfitDistribution;
import com.example.domain.model.ProfitPeriodBalance;
import com.example.domain.model.TaxRegime;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcFiscalAccountingRepository implements FiscalAccountingRepository {
    @Inject
    DataSource dataSource;

    @Override
    public FiscalProfile upsertProfile(UpsertFiscalProfileCommand command) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int updated = updateProfile(connection, command);
                if (updated == 0) {
                    insertProfile(connection, command);
                }
                FiscalProfile profile = findProfile(connection, command.tenantId())
                        .orElseThrow(() -> new RepositoryException("Could not find saved fiscal profile", null));
                connection.commit();
                return profile;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not upsert fiscal profile", exception);
        }
    }

    @Override
    public Optional<FiscalProfile> findProfile(String tenantId) {
        try (Connection connection = dataSource.getConnection()) {
            return findProfile(connection, tenantId);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find fiscal profile", exception);
        }
    }

    @Override
    public ExpenseEntry createExpense(CreateExpenseCommand command) {
        try (Connection connection = dataSource.getConnection()) {
            ensurePeriodOpen(connection, command.tenantId(), command.expenseDate());
            String id = UUID.randomUUID().toString();
            Timestamp now = Timestamp.from(java.time.Instant.now());
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO expense_entries
                    (id, tenant_id, expense_date, category, description, amount,
                     attachment_public_id, attachment_secure_url, attachment_resource_type,
                     attachment_original_filename, attachment_content_type, attachment_size_bytes,
                     created_at, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, id);
                bindExpenseValues(statement, command, now, 2);
                statement.executeUpdate();
            }
            return findExpense(connection, command.tenantId(), id)
                    .orElseThrow(() -> new RepositoryException("Could not find saved expense", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not create expense", exception);
        }
    }

    @Override
    public ExpenseEntry updateExpense(UpdateExpenseCommand command) {
        try (Connection connection = dataSource.getConnection()) {
            ensurePeriodOpen(connection, command.tenantId(), command.expenseDate());
            Timestamp now = Timestamp.from(java.time.Instant.now());
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE expense_entries
                    SET expense_date = ?,
                        category = ?,
                        description = ?,
                        amount = ?,
                        attachment_public_id = ?,
                        attachment_secure_url = ?,
                        attachment_resource_type = ?,
                        attachment_original_filename = ?,
                        attachment_content_type = ?,
                        attachment_size_bytes = ?,
                        updated_at = ?
                    WHERE tenant_id = ? AND id = ?
                    """)) {
                statement.setDate(1, Date.valueOf(command.expenseDate()));
                statement.setString(2, command.category().name());
                statement.setString(3, command.description());
                statement.setBigDecimal(4, command.amount());
                bindAttachment(statement, command.attachment(), 5);
                statement.setTimestamp(11, now);
                statement.setString(12, command.tenantId());
                statement.setString(13, command.expenseId());
                int updated = statement.executeUpdate();
                if (updated == 0) {
                    throw new NotFoundException("expense_not_found");
                }
            }
            return findExpense(connection, command.tenantId(), command.expenseId())
                    .orElseThrow(() -> new NotFoundException("expense_not_found"));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not update expense", exception);
        }
    }

    @Override
    public Optional<ExpenseEntry> findExpense(String tenantId, String expenseId) {
        try (Connection connection = dataSource.getConnection()) {
            return findExpense(connection, tenantId, expenseId);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find expense", exception);
        }
    }

    @Override
    public ExpensePage searchExpenses(String tenantId, ExpenseFilter filter) {
        ExpenseFilter safeFilter = safeFilter(filter);
        FilterSql filterSql = filterSql(tenantId, safeFilter);
        long total = countByFilter(filterSql);
        String sql = """
                SELECT id, tenant_id, expense_date, category, description, amount,
                       attachment_public_id, attachment_secure_url, attachment_resource_type,
                       attachment_original_filename, attachment_content_type, attachment_size_bytes,
                       created_at, updated_at
                FROM expense_entries
                """ + filterSql.whereClause() + """
                ORDER BY expense_date DESC, created_at DESC, id ASC
                LIMIT ? OFFSET ?
                """;
        List<Object> parameters = new ArrayList<>(filterSql.parameters());
        parameters.add(safeFilter.safeSize());
        parameters.add(safeFilter.offset());

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            List<ExpenseEntry> expenses = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    expenses.add(readExpense(resultSet));
                }
            }
            return new ExpensePage(expenses, total, safeFilter.safePage(), safeFilter.safeSize());
        } catch (SQLException exception) {
            throw new RepositoryException("Could not search expenses", exception);
        }
    }

    @Override
    public BigDecimal sumExpenses(String tenantId, ExpenseFilter filter) {
        FilterSql filterSql = deductibleFilterSql(tenantId, safeFilter(filter));
        String sql = "SELECT COALESCE(SUM(amount), 0) AS total FROM expense_entries " + filterSql.whereClause();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, filterSql.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                BigDecimal total = resultSet.getBigDecimal("total");
                return total == null ? BigDecimal.ZERO : total;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not sum expenses", exception);
        }
    }

    @Override
    public long countExpenses(String tenantId, ExpenseFilter filter) {
        return countByFilter(deductibleFilterSql(tenantId, safeFilter(filter)));
    }

    @Override
    public List<ExpenseCategoryTotal> sumExpensesByCategory(String tenantId, ExpenseFilter filter) {
        FilterSql filterSql = deductibleFilterSql(tenantId, safeFilter(filter));
        String sql = """
                SELECT category, COALESCE(SUM(amount), 0) AS amount, COUNT(*) AS entry_count
                FROM expense_entries
                """ + filterSql.whereClause() + """

                GROUP BY category
                ORDER BY category ASC
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, filterSql.parameters());
            List<ExpenseCategoryTotal> totals = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    totals.add(new ExpenseCategoryTotal(
                            readCategory(resultSet.getString("category")),
                            resultSet.getBigDecimal("amount"),
                            resultSet.getLong("entry_count")
                    ));
                }
            }
            return totals;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not sum expenses by category", exception);
        }
    }

    @Override
    public boolean deleteExpense(String tenantId, String expenseId) {
        try (Connection connection = dataSource.getConnection()) {
            Optional<ExpenseEntry> existing = findExpense(connection, tenantId, expenseId);
            if (existing.isEmpty()) {
                return false;
            }
            ensurePeriodOpen(connection, tenantId, existing.get().expenseDate());
            try (PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM expense_entries
                     WHERE tenant_id = ? AND id = ?
                     """)) {
                statement.setString(1, tenantId);
                statement.setString(2, expenseId);
                return statement.executeUpdate() > 0;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not delete expense", exception);
        }
    }

    @Override
    public AccountingPeriodClosing signClosing(SignAccountingPeriodCommand command) {
        try (Connection connection = dataSource.getConnection()) {
            Optional<AccountingPeriodClosing> existing = findClosing(connection, command.tenantId(), command.periodMonth());
            if (existing.isPresent()) {
                return existing.get();
            }
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO accounting_period_closings
                    (tenant_id, period_month, signed_by_user_id, signed_by_email, signature_hash, signed_at, distributable_profit)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, command.tenantId());
                statement.setDate(2, Date.valueOf(periodStart(command.periodMonth())));
                statement.setString(3, command.signedByUserId());
                statement.setString(4, command.signedByEmail());
                statement.setString(5, command.signatureHash());
                statement.setTimestamp(6, Timestamp.from(java.time.Instant.now()));
                statement.setBigDecimal(7, command.distributableProfit());
                statement.executeUpdate();
            }
            return findClosing(connection, command.tenantId(), command.periodMonth())
                    .orElseThrow(() -> new RepositoryException("Could not find saved accounting closing", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not sign accounting period", exception);
        }
    }

    @Override
    public Optional<AccountingPeriodClosing> findClosing(String tenantId, YearMonth periodMonth) {
        try (Connection connection = dataSource.getConnection()) {
            return findClosing(connection, tenantId, periodMonth);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find accounting closing", exception);
        }
    }

    @Override
    public boolean isPeriodClosed(String tenantId, LocalDate date) {
        if (tenantId == null || tenantId.isBlank() || date == null) {
            return false;
        }
        try (Connection connection = dataSource.getConnection()) {
            return isPeriodClosed(connection, tenantId, date);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not verify accounting period", exception);
        }
    }

    @Override
    public ProfitAvailability profitAvailability(String tenantId) {
        try (Connection connection = dataSource.getConnection()) {
            List<ProfitPeriodBalance> periods = new ArrayList<>();
            BigDecimal totalReleased = BigDecimal.ZERO;
            BigDecimal totalDistributed = BigDecimal.ZERO;
            try (PreparedStatement statement = connection.prepareStatement("""
                    SELECT c.period_month,
                           c.signed_at,
                           c.distributable_profit,
                           COALESCE(SUM(d.amount), 0) AS distributed_profit
                    FROM accounting_period_closings c
                    LEFT JOIN profit_distributions d
                        ON d.tenant_id = c.tenant_id
                       AND d.period_month = c.period_month
                    WHERE c.tenant_id = ?
                    GROUP BY c.period_month, c.signed_at, c.distributable_profit
                    ORDER BY c.period_month DESC
                    """)) {
                statement.setString(1, tenantId);
                try (ResultSet resultSet = statement.executeQuery()) {
                    while (resultSet.next()) {
                        BigDecimal distributable = money(resultSet.getBigDecimal("distributable_profit"));
                        BigDecimal distributed = money(resultSet.getBigDecimal("distributed_profit"));
                        BigDecimal available = distributable.subtract(distributed);
                        if (available.compareTo(BigDecimal.ZERO) < 0) {
                            available = BigDecimal.ZERO;
                        }
                        totalReleased = totalReleased.add(distributable);
                        totalDistributed = totalDistributed.add(distributed);
                        periods.add(new ProfitPeriodBalance(
                                YearMonth.from(resultSet.getDate("period_month").toLocalDate()).toString(),
                                resultSet.getTimestamp("signed_at").toInstant(),
                                distributable,
                                distributed,
                                money(available)
                        ));
                    }
                }
            }
            BigDecimal available = totalReleased.subtract(totalDistributed);
            if (available.compareTo(BigDecimal.ZERO) < 0) {
                available = BigDecimal.ZERO;
            }
            return new ProfitAvailability(tenantId, money(totalReleased), money(totalDistributed), money(available), periods);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not calculate profit availability", exception);
        }
    }

    @Override
    public List<ProfitDistribution> listProfitDistributions(String tenantId, YearMonth periodMonth) {
        String sql = """
                SELECT id, tenant_id, period_month, amount, distributed_at, recipient_name,
                       notes, created_by_user_id, created_by_email, created_at
                FROM profit_distributions
                WHERE tenant_id = ?
                """ + (periodMonth == null ? "" : " AND period_month = ? ") + """
                ORDER BY distributed_at DESC, created_at DESC, id ASC
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tenantId);
            if (periodMonth != null) {
                statement.setDate(2, Date.valueOf(periodStart(periodMonth)));
            }
            List<ProfitDistribution> distributions = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    distributions.add(readProfitDistribution(resultSet));
                }
            }
            return distributions;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list profit distributions", exception);
        }
    }

    @Override
    public ProfitDistribution createProfitDistribution(CreateProfitDistributionCommand command) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                AccountingPeriodClosing closing = findClosing(connection, command.tenantId(), command.periodMonth())
                        .orElseThrow(() -> new NotFoundException("accounting_period_closing_not_found"));
                BigDecimal distributed = sumDistributedProfit(connection, command.tenantId(), command.periodMonth());
                BigDecimal available = money(closing.distributableProfit()).subtract(distributed);
                if (command.amount().compareTo(available) > 0) {
                    throw new ValidationException("insufficient_distributable_profit");
                }

                String id = UUID.randomUUID().toString();
                Timestamp now = Timestamp.from(Instant.now());
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO profit_distributions
                        (id, tenant_id, period_month, amount, distributed_at, recipient_name, notes,
                         created_by_user_id, created_by_email, created_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    statement.setString(1, id);
                    statement.setString(2, command.tenantId());
                    statement.setDate(3, Date.valueOf(periodStart(command.periodMonth())));
                    statement.setBigDecimal(4, command.amount());
                    statement.setDate(5, Date.valueOf(command.distributedAt()));
                    statement.setString(6, command.recipientName());
                    statement.setString(7, command.notes());
                    statement.setString(8, command.createdByUserId());
                    statement.setString(9, command.createdByEmail());
                    statement.setTimestamp(10, now);
                    statement.executeUpdate();
                }
                ProfitDistribution distribution = findProfitDistribution(connection, command.tenantId(), id)
                        .orElseThrow(() -> new RepositoryException("Could not find saved profit distribution", null));
                connection.commit();
                return distribution;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not create profit distribution", exception);
        }
    }

    private int updateProfile(Connection connection, UpsertFiscalProfileCommand command) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE tenant_fiscal_profiles
                SET tax_regime = ?,
                    estimated_tax_rate = ?,
                    notes = ?,
                    updated_at = ?
                WHERE tenant_id = ?
                """)) {
            statement.setString(1, command.taxRegime().name());
            statement.setBigDecimal(2, command.estimatedTaxRate());
            statement.setString(3, command.notes());
            statement.setTimestamp(4, Timestamp.from(java.time.Instant.now()));
            statement.setString(5, command.tenantId());
            return statement.executeUpdate();
        }
    }

    private void insertProfile(Connection connection, UpsertFiscalProfileCommand command) throws SQLException {
        Timestamp now = Timestamp.from(java.time.Instant.now());
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tenant_fiscal_profiles
                (tenant_id, tax_regime, estimated_tax_rate, notes, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, command.tenantId());
            statement.setString(2, command.taxRegime().name());
            statement.setBigDecimal(3, command.estimatedTaxRate());
            statement.setString(4, command.notes());
            statement.setTimestamp(5, now);
            statement.setTimestamp(6, now);
            statement.executeUpdate();
        }
    }

    private Optional<FiscalProfile> findProfile(Connection connection, String tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT tenant_id, tax_regime, estimated_tax_rate, notes, created_at, updated_at
                FROM tenant_fiscal_profiles
                WHERE tenant_id = ?
                """)) {
            statement.setString(1, tenantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new FiscalProfile(
                        resultSet.getString("tenant_id"),
                        TaxRegime.valueOf(resultSet.getString("tax_regime")),
                        resultSet.getBigDecimal("estimated_tax_rate"),
                        resultSet.getString("notes"),
                        resultSet.getTimestamp("created_at").toInstant(),
                        resultSet.getTimestamp("updated_at").toInstant()
                ));
            }
        }
    }

    private void bindExpenseValues(PreparedStatement statement, CreateExpenseCommand command, Timestamp now, int startIndex) throws SQLException {
        statement.setString(startIndex, command.tenantId());
        statement.setDate(startIndex + 1, Date.valueOf(command.expenseDate()));
        statement.setString(startIndex + 2, command.category().name());
        statement.setString(startIndex + 3, command.description());
        statement.setBigDecimal(startIndex + 4, command.amount());
        bindAttachment(statement, command.attachment(), startIndex + 5);
        statement.setTimestamp(startIndex + 11, now);
        statement.setTimestamp(startIndex + 12, now);
    }

    private void bindAttachment(PreparedStatement statement, ExpenseAttachment attachment, int startIndex) throws SQLException {
        if (attachment == null) {
            statement.setString(startIndex, null);
            statement.setString(startIndex + 1, null);
            statement.setString(startIndex + 2, null);
            statement.setString(startIndex + 3, null);
            statement.setString(startIndex + 4, null);
            statement.setObject(startIndex + 5, null);
            return;
        }
        statement.setString(startIndex, attachment.publicId());
        statement.setString(startIndex + 1, attachment.secureUrl());
        statement.setString(startIndex + 2, attachment.resourceType());
        statement.setString(startIndex + 3, attachment.originalFilename());
        statement.setString(startIndex + 4, attachment.contentType());
        statement.setObject(startIndex + 5, attachment.sizeBytes());
    }

    private Optional<ExpenseEntry> findExpense(Connection connection, String tenantId, String expenseId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, tenant_id, expense_date, category, description, amount,
                       attachment_public_id, attachment_secure_url, attachment_resource_type,
                       attachment_original_filename, attachment_content_type, attachment_size_bytes,
                       created_at, updated_at
                FROM expense_entries
                WHERE tenant_id = ? AND id = ?
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, expenseId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readExpense(resultSet));
            }
        }
    }

    private ExpenseEntry readExpense(ResultSet resultSet) throws SQLException {
        return new ExpenseEntry(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                resultSet.getDate("expense_date").toLocalDate(),
                readCategory(resultSet.getString("category")),
                resultSet.getString("description"),
                resultSet.getBigDecimal("amount"),
                readAttachment(resultSet),
                readInstant(resultSet, "created_at"),
                readInstant(resultSet, "updated_at")
        );
    }

    private ExpenseCategory readCategory(String value) {
        if (value == null || value.isBlank()) {
            return ExpenseCategory.OTHER;
        }
        try {
            return ExpenseCategory.valueOf(value.trim());
        } catch (IllegalArgumentException exception) {
            return ExpenseCategory.OTHER;
        }
    }

    private Instant readInstant(ResultSet resultSet, String column) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(column);
        return timestamp == null ? Instant.EPOCH : timestamp.toInstant();
    }

    private ExpenseAttachment readAttachment(ResultSet resultSet) throws SQLException {
        String publicId = resultSet.getString("attachment_public_id");
        String secureUrl = resultSet.getString("attachment_secure_url");
        if (publicId == null && secureUrl == null) {
            return null;
        }
        Long sizeBytes = null;
        long rawSize = resultSet.getLong("attachment_size_bytes");
        if (!resultSet.wasNull()) {
            sizeBytes = rawSize;
        }
        return new ExpenseAttachment(
                publicId,
                secureUrl,
                resultSet.getString("attachment_resource_type"),
                resultSet.getString("attachment_original_filename"),
                resultSet.getString("attachment_content_type"),
                sizeBytes
        );
    }

    private long countByFilter(FilterSql filterSql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM expense_entries " + filterSql.whereClause())) {
            bind(statement, filterSql.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not count expenses", exception);
        }
    }

    private FilterSql filterSql(String tenantId, ExpenseFilter filter) {
        StringBuilder where = new StringBuilder(" WHERE tenant_id = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(tenantId);
        if (filter.from() != null) {
            where.append(" AND expense_date >= ?");
            parameters.add(filter.from());
        }
        if (filter.to() != null) {
            where.append(" AND expense_date <= ?");
            parameters.add(filter.to());
        }
        if (filter.category() != null) {
            where.append(" AND category = ?");
            parameters.add(filter.category());
        }
        return new FilterSql(where.toString(), parameters);
    }

    private FilterSql deductibleFilterSql(String tenantId, ExpenseFilter filter) {
        FilterSql filterSql = filterSql(tenantId, filter);
        return new FilterSql(
                filterSql.whereClause()
                        + " AND attachment_public_id IS NOT NULL"
                        + " AND attachment_secure_url IS NOT NULL",
                filterSql.parameters()
        );
    }

    private ExpenseFilter safeFilter(ExpenseFilter filter) {
        return filter == null ? new ExpenseFilter(null, null, null, null, null) : filter;
    }

    private Optional<AccountingPeriodClosing> findClosing(Connection connection, String tenantId, YearMonth periodMonth) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT tenant_id, period_month, signed_by_user_id, signed_by_email, signature_hash, signed_at, distributable_profit
                FROM accounting_period_closings
                WHERE tenant_id = ? AND period_month = ?
                """)) {
            statement.setString(1, tenantId);
            statement.setDate(2, Date.valueOf(periodStart(periodMonth)));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readClosing(resultSet));
            }
        }
    }

    private AccountingPeriodClosing readClosing(ResultSet resultSet) throws SQLException {
        LocalDate periodStart = resultSet.getDate("period_month").toLocalDate();
        return new AccountingPeriodClosing(
                resultSet.getString("tenant_id"),
                YearMonth.from(periodStart).toString(),
                resultSet.getString("signed_by_user_id"),
                resultSet.getString("signed_by_email"),
                resultSet.getString("signature_hash"),
                resultSet.getTimestamp("signed_at").toInstant(),
                resultSet.getBigDecimal("distributable_profit")
        );
    }

    private Optional<ProfitDistribution> findProfitDistribution(Connection connection, String tenantId, String id) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, tenant_id, period_month, amount, distributed_at, recipient_name,
                       notes, created_by_user_id, created_by_email, created_at
                FROM profit_distributions
                WHERE tenant_id = ? AND id = ?
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readProfitDistribution(resultSet));
            }
        }
    }

    private ProfitDistribution readProfitDistribution(ResultSet resultSet) throws SQLException {
        return new ProfitDistribution(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                YearMonth.from(resultSet.getDate("period_month").toLocalDate()).toString(),
                money(resultSet.getBigDecimal("amount")),
                resultSet.getDate("distributed_at").toLocalDate(),
                resultSet.getString("recipient_name"),
                resultSet.getString("notes"),
                resultSet.getString("created_by_user_id"),
                resultSet.getString("created_by_email"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }

    private BigDecimal sumDistributedProfit(Connection connection, String tenantId, YearMonth periodMonth) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COALESCE(SUM(amount), 0) AS total
                FROM profit_distributions
                WHERE tenant_id = ? AND period_month = ?
                """)) {
            statement.setString(1, tenantId);
            statement.setDate(2, Date.valueOf(periodStart(periodMonth)));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return money(resultSet.getBigDecimal("total"));
            }
        }
    }

    private BigDecimal money(BigDecimal value) {
        return value == null ? BigDecimal.ZERO.setScale(2) : value.setScale(2);
    }

    private void ensurePeriodOpen(Connection connection, String tenantId, LocalDate date) throws SQLException {
        if (isPeriodClosed(connection, tenantId, date)) {
            throw new AccountingPeriodClosedException("accounting_period_closed");
        }
    }

    private boolean isPeriodClosed(Connection connection, String tenantId, LocalDate date) throws SQLException {
        if (tenantId == null || tenantId.isBlank() || date == null) {
            return false;
        }
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT COUNT(*)
                FROM accounting_period_closings
                WHERE tenant_id = ? AND period_month = ?
                """)) {
            statement.setString(1, tenantId);
            statement.setDate(2, Date.valueOf(periodStart(YearMonth.from(date))));
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1) > 0;
            }
        }
    }

    private LocalDate periodStart(YearMonth periodMonth) {
        return periodMonth.atDay(1);
    }

    private void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object value = parameters.get(index);
            int position = index + 1;
            if (value instanceof LocalDate localDate) {
                statement.setDate(position, Date.valueOf(localDate));
            } else if (value instanceof ExpenseCategory category) {
                statement.setString(position, category.name());
            } else if (value instanceof Integer integer) {
                statement.setInt(position, integer);
            } else {
                statement.setObject(position, value);
            }
        }
    }

    private record FilterSql(String whereClause, List<Object> parameters) {
        private FilterSql {
            whereClause = (whereClause == null ? "" : whereClause.stripTrailing()) + " ";
            parameters = List.copyOf(parameters);
        }
    }
}
