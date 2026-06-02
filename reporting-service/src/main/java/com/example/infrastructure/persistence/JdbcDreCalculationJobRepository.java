package com.example.infrastructure.persistence;

import com.example.application.event.DreCalculationRequestedEvent;
import com.example.application.port.out.DreCalculationJobRepository;
import com.example.domain.model.DreCalculationJob;
import com.example.domain.model.DreCalculationStatus;
import com.example.domain.model.DreStatement;
import com.example.domain.model.ExpenseCategoryTotal;
import com.example.domain.model.TaxRegime;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcDreCalculationJobRepository implements DreCalculationJobRepository {
    private static final long STALE_PROCESSING_TIMEOUT_SECONDS = 900;

    private static final TypeReference<List<ExpenseCategoryTotal>> EXPENSE_CATEGORY_TOTALS = new TypeReference<>() {
    };

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public DreCalculationJob createQueued(DreCalculationRequestedEvent event) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO dre_calculation_jobs
                    (id, tenant_id, period_from, period_to, status,
                     requested_by_user_id, requested_by_email, requested_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, event.jobId());
                statement.setString(2, event.tenantId());
                statement.setDate(3, Date.valueOf(event.from()));
                statement.setDate(4, Date.valueOf(event.to()));
                statement.setString(5, DreCalculationStatus.QUEUED.name());
                statement.setString(6, event.requestedByUserId());
                statement.setString(7, event.requestedByEmail());
                statement.setTimestamp(8, Timestamp.from(event.requestedAt()));
                statement.executeUpdate();
            }
            return find(connection, event.tenantId(), event.jobId())
                    .orElseThrow(() -> new RepositoryException("Could not find queued DRE job", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not create DRE calculation job", exception);
        }
    }

    @Override
    public void markProcessing(String jobId) {
        tryMarkProcessing(jobId);
    }

    @Override
    public boolean tryMarkProcessing(String jobId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE dre_calculation_jobs
                     SET status = ?,
                         started_at = ?,
                         error_message = NULL
                     WHERE id = ?
                       AND (status = ?
                         OR (status = ? AND started_at < ?))
                     """)) {
            Timestamp now = Timestamp.from(Instant.now());
            Timestamp staleBefore = Timestamp.from(Instant.now().minusSeconds(STALE_PROCESSING_TIMEOUT_SECONDS));
            statement.setString(1, DreCalculationStatus.PROCESSING.name());
            statement.setTimestamp(2, now);
            statement.setString(3, jobId);
            statement.setString(4, DreCalculationStatus.QUEUED.name());
            statement.setString(5, DreCalculationStatus.PROCESSING.name());
            statement.setTimestamp(6, staleBefore);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not mark DRE calculation job as processing", exception);
        }
    }

    @Override
    public DreCalculationJob markCompleted(String jobId, DreStatement statement) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement preparedStatement = connection.prepareStatement("""
                    UPDATE dre_calculation_jobs
                    SET status = ?,
                        finished_at = ?,
                        error_message = NULL,
                        tax_regime = ?,
                        estimated_tax_rate = ?,
                        gross_revenue = ?,
                        marketplace_fees = ?,
                        estimated_taxes = ?,
                        operating_expenses = ?,
                        net_result = ?,
                        sales_count = ?,
                        expense_count = ?,
                        expenses_by_category_json = ?
                    WHERE id = ?
                    """)) {
                preparedStatement.setString(1, DreCalculationStatus.COMPLETED.name());
                preparedStatement.setTimestamp(2, Timestamp.from(Instant.now()));
                preparedStatement.setString(3, statement.taxRegime() == null ? null : statement.taxRegime().name());
                preparedStatement.setBigDecimal(4, statement.estimatedTaxRate());
                preparedStatement.setBigDecimal(5, statement.grossRevenue());
                preparedStatement.setBigDecimal(6, statement.marketplaceFees());
                preparedStatement.setBigDecimal(7, statement.estimatedTaxes());
                preparedStatement.setBigDecimal(8, statement.operatingExpenses());
                preparedStatement.setBigDecimal(9, statement.netResult());
                preparedStatement.setLong(10, statement.salesCount());
                preparedStatement.setLong(11, statement.expenseCount());
                preparedStatement.setString(12, expensesJson(statement.expensesByCategory()));
                preparedStatement.setString(13, jobId);
                preparedStatement.executeUpdate();
            }
            return find(connection, statement.tenantId(), jobId)
                    .orElseThrow(() -> new RepositoryException("Could not find completed DRE job", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not mark DRE calculation job as completed", exception);
        }
    }

    @Override
    public DreCalculationJob markFailed(String jobId, String errorMessage) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE dre_calculation_jobs
                    SET status = ?,
                        finished_at = ?,
                        error_message = ?
                    WHERE id = ?
                    """)) {
                statement.setString(1, DreCalculationStatus.FAILED.name());
                statement.setTimestamp(2, Timestamp.from(Instant.now()));
                statement.setString(3, truncate(errorMessage, 600));
                statement.setString(4, jobId);
                statement.executeUpdate();
            }
            return findById(connection, jobId)
                    .orElseThrow(() -> new RepositoryException("Could not find failed DRE job", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not mark DRE calculation job as failed", exception);
        }
    }

    @Override
    public Optional<DreCalculationJob> find(String tenantId, String jobId) {
        try (Connection connection = dataSource.getConnection()) {
            return find(connection, tenantId, jobId);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find DRE calculation job", exception);
        }
    }

    private Optional<DreCalculationJob> find(Connection connection, String tenantId, String jobId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(selectSql() + " WHERE tenant_id = ? AND id = ?")) {
            statement.setString(1, tenantId);
            statement.setString(2, jobId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readJob(resultSet));
            }
        }
    }

    private Optional<DreCalculationJob> findById(Connection connection, String jobId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement(selectSql() + " WHERE id = ?")) {
            statement.setString(1, jobId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readJob(resultSet));
            }
        }
    }

    private String selectSql() {
        return """
                SELECT id, tenant_id, period_from, period_to, status,
                       requested_by_user_id, requested_by_email, requested_at,
                       started_at, finished_at, error_message,
                       tax_regime, estimated_tax_rate, gross_revenue, marketplace_fees,
                       estimated_taxes, operating_expenses, net_result,
                       sales_count, expense_count, expenses_by_category_json
                FROM dre_calculation_jobs
                """;
    }

    private DreCalculationJob readJob(ResultSet resultSet) throws SQLException {
        String tenantId = resultSet.getString("tenant_id");
        LocalDate from = resultSet.getDate("period_from").toLocalDate();
        LocalDate to = resultSet.getDate("period_to").toLocalDate();
        DreCalculationStatus status = DreCalculationStatus.valueOf(resultSet.getString("status"));
        return new DreCalculationJob(
                resultSet.getString("id"),
                tenantId,
                from,
                to,
                status,
                resultSet.getString("requested_by_user_id"),
                resultSet.getString("requested_by_email"),
                resultSet.getTimestamp("requested_at").toInstant(),
                instant(resultSet, "started_at"),
                instant(resultSet, "finished_at"),
                resultSet.getString("error_message"),
                readStatement(resultSet, tenantId, from, to, status)
        );
    }

    private DreStatement readStatement(
            ResultSet resultSet,
            String tenantId,
            LocalDate from,
            LocalDate to,
            DreCalculationStatus status) throws SQLException {
        if (status != DreCalculationStatus.COMPLETED) {
            return null;
        }
        return new DreStatement(
                tenantId,
                from,
                to,
                taxRegime(resultSet.getString("tax_regime")),
                money(resultSet, "estimated_tax_rate"),
                money(resultSet, "gross_revenue"),
                money(resultSet, "marketplace_fees"),
                money(resultSet, "estimated_taxes"),
                money(resultSet, "operating_expenses"),
                money(resultSet, "net_result"),
                resultSet.getLong("sales_count"),
                resultSet.getLong("expense_count"),
                expenses(resultSet.getString("expenses_by_category_json"))
        );
    }

    private TaxRegime taxRegime(String value) {
        return value == null || value.isBlank() ? null : TaxRegime.valueOf(value);
    }

    private BigDecimal money(ResultSet resultSet, String columnName) throws SQLException {
        BigDecimal value = resultSet.getBigDecimal(columnName);
        return value == null ? BigDecimal.ZERO : value;
    }

    private Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private List<ExpenseCategoryTotal> expenses(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, EXPENSE_CATEGORY_TOTALS);
        } catch (JsonProcessingException exception) {
            throw new RepositoryException("Could not read DRE expense categories", exception);
        }
    }

    private String expensesJson(List<ExpenseCategoryTotal> expenses) {
        try {
            return objectMapper.writeValueAsString(expenses == null ? List.of() : expenses);
        } catch (JsonProcessingException exception) {
            throw new RepositoryException("Could not write DRE expense categories", exception);
        }
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
