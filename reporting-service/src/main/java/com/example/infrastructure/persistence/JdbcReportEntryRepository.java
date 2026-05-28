package com.example.infrastructure.persistence;

import com.example.application.command.UpsertReportEntryCommand;
import com.example.application.port.out.ReportEntryRepository;
import com.example.domain.model.AvailableFilters;
import com.example.domain.model.FinancialSummary;
import com.example.domain.model.MonthlyEvolutionPoint;
import com.example.domain.model.PaymentMethod;
import com.example.domain.model.PaymentReleaseAlert;
import com.example.domain.model.PlatformComparisonPoint;
import com.example.domain.model.ReportEntry;
import com.example.domain.model.ReportEntryPage;
import com.example.domain.model.ReportEntryStatus;
import com.example.domain.model.ReportFilter;
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
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcReportEntryRepository implements ReportEntryRepository {
    @Inject
    DataSource dataSource;

    @Override
    public ReportEntry upsert(UpsertReportEntryCommand command) {
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int updated = updateEntry(connection, command);
                if (updated == 0) {
                    insertEntry(connection, command);
                }
                ReportEntry entry = findByNaturalKey(connection, command.tenantId(), command.platform(), command.orderId())
                        .orElseThrow(() -> new RepositoryException("Could not find saved report entry", null));
                connection.commit();
                return entry;
            } catch (SQLException | RuntimeException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not upsert report entry", exception);
        }
    }

    @Override
    public FinancialSummary summarize(String tenantId, ReportFilter filter) {
        FilterSql filterSql = filterSql(tenantId, filter);
        String sql = """
                SELECT
                    COALESCE(SUM(gross_value), 0) AS gross_value,
                    COALESCE(SUM(received_value), 0) AS received_value,
                    COALESCE(SUM(fee_value), 0) AS fee_value,
                    COALESCE(SUM(receivable_value), 0) AS receivable_value,
                    COUNT(*) AS entry_count
                FROM report_entries
                """ + filterSql.whereClause();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, filterSql.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return new FinancialSummary(
                        tenantId,
                        money(resultSet, "gross_value"),
                        money(resultSet, "received_value"),
                        money(resultSet, "fee_value"),
                        money(resultSet, "receivable_value"),
                        resultSet.getLong("entry_count")
                );
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not summarize report entries", exception);
        }
    }

    @Override
    public ReportEntryPage search(String tenantId, ReportFilter filter) {
        ReportFilter safeFilter = safeFilter(filter);
        FilterSql filterSql = filterSql(tenantId, safeFilter);
        long total = count(filterSql);
        String sql = """
                SELECT id, tenant_id, platform, order_id, sale_date, gross_value, received_value,
                       fee_value, receivable_value, payment_method, status, release_date,
                       buyer_name, invoice_number, created_at, updated_at
                FROM report_entries
                """ + filterSql.whereClause()
                + " ORDER BY " + safeFilter.sortColumn() + " " + safeFilter.sortDirection()
                + ", id ASC LIMIT ? OFFSET ?";

        List<Object> parameters = new ArrayList<>(filterSql.parameters());
        parameters.add(safeFilter.safeSize());
        parameters.add(safeFilter.offset());

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            List<ReportEntry> entries = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(readEntry(resultSet));
                }
            }
            return new ReportEntryPage(entries, total, safeFilter.safePage(), safeFilter.safeSize());
        } catch (SQLException exception) {
            throw new RepositoryException("Could not search report entries", exception);
        }
    }

    @Override
    public List<ReportEntry> listForExport(String tenantId, ReportFilter filter) {
        FilterSql filterSql = filterSql(tenantId, filter);
        String sql = """
                SELECT id, tenant_id, platform, order_id, sale_date, gross_value, received_value,
                       fee_value, receivable_value, payment_method, status, release_date,
                       buyer_name, invoice_number, created_at, updated_at
                FROM report_entries
                """ + filterSql.whereClause() + """
                ORDER BY platform ASC, sale_date ASC, order_id ASC, id ASC
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, filterSql.parameters());
            List<ReportEntry> entries = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    entries.add(readEntry(resultSet));
                }
            }
            return entries;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list report entries for export", exception);
        }
    }

    @Override
    public List<PaymentReleaseAlert> paymentReleases(String tenantId, String platform, LocalDate from, LocalDate to) {
        StringBuilder where = new StringBuilder("""
                 WHERE tenant_id = ?
                   AND release_date IS NOT NULL
                   AND receivable_value > 0
                   AND status IN ('PAID', 'PENDING_RELEASE')
                """);
        List<Object> parameters = new ArrayList<>();
        parameters.add(tenantId);
        String normalizedPlatform = platform == null || platform.isBlank() ? null : platform.trim().toLowerCase(Locale.ROOT);
        if (normalizedPlatform != null) {
            where.append(" AND LOWER(platform) = ?");
            parameters.add(normalizedPlatform);
        }
        if (from != null) {
            where.append(" AND release_date >= ?");
            parameters.add(from);
        }
        if (to != null) {
            where.append(" AND release_date <= ?");
            parameters.add(to);
        }

        String sql = """
                SELECT tenant_id, platform, order_id, receivable_value, release_date
                FROM report_entries
                """ + where + """
                ORDER BY release_date ASC, platform ASC, order_id ASC
                """;

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, parameters);
            List<PaymentReleaseAlert> releases = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    releases.add(new PaymentReleaseAlert(
                            resultSet.getString("tenant_id"),
                            resultSet.getString("platform"),
                            resultSet.getString("order_id"),
                            money(resultSet, "receivable_value"),
                            resultSet.getDate("release_date").toLocalDate()
                    ));
                }
            }
            return releases;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list payment releases", exception);
        }
    }

    @Override
    public List<MonthlyEvolutionPoint> monthlyEvolution(String tenantId, ReportFilter filter) {
        FilterSql filterSql = filterSql(tenantId, filter);
        String sql = """
                SELECT
                    EXTRACT(YEAR FROM sale_date) AS year_value,
                    EXTRACT(MONTH FROM sale_date) AS month_value,
                    COALESCE(SUM(gross_value), 0) AS gross_value,
                    COALESCE(SUM(received_value), 0) AS received_value,
                    COALESCE(SUM(fee_value), 0) AS fee_value,
                    COALESCE(SUM(receivable_value), 0) AS receivable_value,
                    COUNT(*) AS entry_count
                FROM report_entries
                """ + filterSql.whereClause() + """
                GROUP BY EXTRACT(YEAR FROM sale_date), EXTRACT(MONTH FROM sale_date)
                ORDER BY year_value ASC, month_value ASC
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, filterSql.parameters());
            List<MonthlyEvolutionPoint> points = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    int year = resultSet.getInt("year_value");
                    int month = resultSet.getInt("month_value");
                    points.add(new MonthlyEvolutionPoint(
                            "%04d-%02d".formatted(year, month),
                            money(resultSet, "gross_value"),
                            money(resultSet, "received_value"),
                            money(resultSet, "fee_value"),
                            money(resultSet, "receivable_value"),
                            resultSet.getLong("entry_count")
                    ));
                }
            }
            return points;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not calculate monthly evolution", exception);
        }
    }

    @Override
    public List<PlatformComparisonPoint> platformComparison(String tenantId, ReportFilter filter) {
        FilterSql filterSql = filterSql(tenantId, filter);
        String sql = """
                SELECT
                    platform,
                    COALESCE(SUM(gross_value), 0) AS gross_value,
                    COALESCE(SUM(received_value), 0) AS received_value,
                    COALESCE(SUM(fee_value), 0) AS fee_value,
                    COALESCE(SUM(receivable_value), 0) AS receivable_value,
                    COUNT(*) AS entry_count
                FROM report_entries
                """ + filterSql.whereClause() + """
                GROUP BY platform
                ORDER BY gross_value DESC, platform ASC
                """;
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bind(statement, filterSql.parameters());
            List<PlatformComparisonPoint> points = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    points.add(new PlatformComparisonPoint(
                            resultSet.getString("platform"),
                            money(resultSet, "gross_value"),
                            money(resultSet, "received_value"),
                            money(resultSet, "fee_value"),
                            money(resultSet, "receivable_value"),
                            resultSet.getLong("entry_count")
                    ));
                }
            }
            return points;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not calculate platform comparison", exception);
        }
    }

    @Override
    public AvailableFilters availableFilters(String tenantId) {
        try (Connection connection = dataSource.getConnection()) {
            return new AvailableFilters(
                    distinctStrings(connection, "platform", tenantId),
                    distinctPaymentMethods(connection, tenantId),
                    distinctStatuses(connection, tenantId)
            );
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list report filters", exception);
        }
    }

    private int updateEntry(Connection connection, UpsertReportEntryCommand command) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE report_entries
                SET sale_date = ?,
                    gross_value = ?,
                    received_value = ?,
                    fee_value = ?,
                    receivable_value = ?,
                    payment_method = ?,
                    status = ?,
                    release_date = ?,
                    buyer_name = ?,
                    invoice_number = ?,
                    updated_at = ?
                WHERE tenant_id = ? AND platform = ? AND order_id = ?
                """)) {
            bindEntryValues(statement, command);
            statement.setString(12, command.tenantId());
            statement.setString(13, command.platform());
            statement.setString(14, command.orderId());
            return statement.executeUpdate();
        }
    }

    private void insertEntry(Connection connection, UpsertReportEntryCommand command) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO report_entries
                (id, tenant_id, platform, order_id, sale_date, gross_value, received_value,
                 fee_value, receivable_value, payment_method, status, release_date,
                 buyer_name, invoice_number, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            Timestamp now = Timestamp.from(java.time.Instant.now());
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, command.tenantId());
            statement.setString(3, command.platform());
            statement.setString(4, command.orderId());
            statement.setDate(5, Date.valueOf(command.saleDate()));
            statement.setBigDecimal(6, command.grossValue());
            statement.setBigDecimal(7, command.receivedValue());
            statement.setBigDecimal(8, command.feeValue());
            statement.setBigDecimal(9, command.receivableValue());
            statement.setString(10, command.paymentMethod().name());
            statement.setString(11, command.status().name());
            statement.setDate(12, date(command.releaseDate()));
            statement.setString(13, command.buyerName());
            statement.setString(14, command.invoiceNumber());
            statement.setTimestamp(15, now);
            statement.setTimestamp(16, now);
            statement.executeUpdate();
        }
    }

    private void bindEntryValues(PreparedStatement statement, UpsertReportEntryCommand command) throws SQLException {
        Timestamp now = Timestamp.from(java.time.Instant.now());
        statement.setDate(1, Date.valueOf(command.saleDate()));
        statement.setBigDecimal(2, command.grossValue());
        statement.setBigDecimal(3, command.receivedValue());
        statement.setBigDecimal(4, command.feeValue());
        statement.setBigDecimal(5, command.receivableValue());
        statement.setString(6, command.paymentMethod().name());
        statement.setString(7, command.status().name());
        statement.setDate(8, date(command.releaseDate()));
        statement.setString(9, command.buyerName());
        statement.setString(10, command.invoiceNumber());
        statement.setTimestamp(11, now);
    }

    private Optional<ReportEntry> findByNaturalKey(Connection connection, String tenantId, String platform, String orderId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, tenant_id, platform, order_id, sale_date, gross_value, received_value,
                       fee_value, receivable_value, payment_method, status, release_date,
                       buyer_name, invoice_number, created_at, updated_at
                FROM report_entries
                WHERE tenant_id = ? AND platform = ? AND order_id = ?
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, platform);
            statement.setString(3, orderId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(readEntry(resultSet));
            }
        }
    }

    private long count(FilterSql filterSql) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("SELECT COUNT(*) FROM report_entries " + filterSql.whereClause())) {
            bind(statement, filterSql.parameters());
            try (ResultSet resultSet = statement.executeQuery()) {
                resultSet.next();
                return resultSet.getLong(1);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not count report entries", exception);
        }
    }

    private FilterSql filterSql(String tenantId, ReportFilter filter) {
        ReportFilter safeFilter = safeFilter(filter);
        StringBuilder where = new StringBuilder(" WHERE tenant_id = ?");
        List<Object> parameters = new ArrayList<>();
        parameters.add(tenantId);

        if (safeFilter.from() != null) {
            where.append(" AND sale_date >= ?");
            parameters.add(safeFilter.from());
        }
        if (safeFilter.to() != null) {
            where.append(" AND sale_date <= ?");
            parameters.add(safeFilter.to());
        }
        if (safeFilter.normalizedPlatform() != null) {
            where.append(" AND LOWER(platform) = ?");
            parameters.add(safeFilter.normalizedPlatform());
        }
        if (safeFilter.paymentMethod() != null) {
            where.append(" AND payment_method = ?");
            parameters.add(safeFilter.paymentMethod());
        }
        if (safeFilter.status() != null) {
            where.append(" AND status = ?");
            parameters.add(safeFilter.status());
        }
        if (safeFilter.normalizedSearch() != null) {
            where.append("""
                     AND (
                        LOWER(order_id) LIKE ?
                        OR LOWER(COALESCE(buyer_name, '')) LIKE ?
                        OR LOWER(COALESCE(invoice_number, '')) LIKE ?
                     )
                    """);
            String pattern = "%" + safeFilter.normalizedSearch() + "%";
            parameters.add(pattern);
            parameters.add(pattern);
            parameters.add(pattern);
        }

        return new FilterSql(where.toString(), parameters);
    }

    private ReportFilter safeFilter(ReportFilter filter) {
        return filter == null
                ? new ReportFilter(null, null, null, null, null, null, null, null, null, null)
                : filter;
    }

    private void bind(PreparedStatement statement, List<Object> parameters) throws SQLException {
        for (int index = 0; index < parameters.size(); index++) {
            Object value = parameters.get(index);
            int position = index + 1;
            if (value instanceof LocalDate localDate) {
                statement.setDate(position, Date.valueOf(localDate));
            } else if (value instanceof PaymentMethod paymentMethod) {
                statement.setString(position, paymentMethod.name());
            } else if (value instanceof ReportEntryStatus status) {
                statement.setString(position, status.name());
            } else if (value instanceof Integer integer) {
                statement.setInt(position, integer);
            } else {
                statement.setObject(position, value);
            }
        }
    }

    private ReportEntry readEntry(ResultSet resultSet) throws SQLException {
        return new ReportEntry(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("platform"),
                resultSet.getString("order_id"),
                resultSet.getDate("sale_date").toLocalDate(),
                money(resultSet, "gross_value"),
                money(resultSet, "received_value"),
                money(resultSet, "fee_value"),
                money(resultSet, "receivable_value"),
                PaymentMethod.valueOf(resultSet.getString("payment_method")),
                ReportEntryStatus.valueOf(resultSet.getString("status")),
                localDate(resultSet, "release_date"),
                resultSet.getString("buyer_name"),
                resultSet.getString("invoice_number"),
                resultSet.getTimestamp("created_at").toInstant(),
                resultSet.getTimestamp("updated_at").toInstant()
        );
    }

    private BigDecimal money(ResultSet resultSet, String columnName) throws SQLException {
        BigDecimal value = resultSet.getBigDecimal(columnName);
        return value == null ? BigDecimal.ZERO : value;
    }

    private LocalDate localDate(ResultSet resultSet, String columnName) throws SQLException {
        Date date = resultSet.getDate(columnName);
        return date == null ? null : date.toLocalDate();
    }

    private Date date(LocalDate value) {
        return value == null ? null : Date.valueOf(value);
    }

    private List<String> distinctStrings(Connection connection, String columnName, String tenantId) throws SQLException {
        String sql = "SELECT DISTINCT " + columnName + " FROM report_entries WHERE tenant_id = ? ORDER BY " + columnName;
        try (PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, tenantId);
            List<String> values = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(resultSet.getString(1));
                }
            }
            return values;
        }
    }

    private List<PaymentMethod> distinctPaymentMethods(Connection connection, String tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT DISTINCT payment_method
                FROM report_entries
                WHERE tenant_id = ?
                ORDER BY payment_method
                """)) {
            statement.setString(1, tenantId);
            List<PaymentMethod> values = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(PaymentMethod.valueOf(resultSet.getString(1).toUpperCase(Locale.ROOT)));
                }
            }
            return values;
        }
    }

    private List<ReportEntryStatus> distinctStatuses(Connection connection, String tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT DISTINCT status
                FROM report_entries
                WHERE tenant_id = ?
                ORDER BY status
                """)) {
            statement.setString(1, tenantId);
            List<ReportEntryStatus> values = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    values.add(ReportEntryStatus.valueOf(resultSet.getString(1).toUpperCase(Locale.ROOT)));
                }
            }
            return values;
        }
    }

    private record FilterSql(String whereClause, List<Object> parameters) {
        private FilterSql {
            parameters = List.copyOf(parameters);
        }
    }
}
