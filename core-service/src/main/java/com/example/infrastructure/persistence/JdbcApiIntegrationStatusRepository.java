package com.example.infrastructure.persistence;

import com.example.application.port.out.ApiIntegrationStatusRepository;
import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;
import com.example.domain.enums.IntegrationHealthStatus;
import com.example.domain.model.monitoring.IntegrationHealthSummary;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class JdbcApiIntegrationStatusRepository implements ApiIntegrationStatusRepository {
    private static final int DOWN_AFTER_CONSECUTIVE_FAILURES = 3;

    @Inject
    DataSource dataSource;

    @Override
    public void applySuccess(String tenantId, String integrationName, int responseTimeMs) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO api_integration_status
                     (tenant_id, integration_name, current_status, last_check_at, last_success_at,
                      consecutive_failures, avg_response_time_ms, updated_at)
                     VALUES (?, ?, 'UP', ?, ?, 0, ?, ?)
                     ON CONFLICT (tenant_id, integration_name) DO UPDATE SET
                         current_status = 'UP',
                         last_check_at = EXCLUDED.last_check_at,
                         last_success_at = EXCLUDED.last_success_at,
                         consecutive_failures = 0,
                         avg_response_time_ms = CASE
                             WHEN api_integration_status.avg_response_time_ms IS NULL THEN EXCLUDED.avg_response_time_ms
                             ELSE (api_integration_status.avg_response_time_ms + EXCLUDED.avg_response_time_ms) / 2
                         END,
                         updated_at = EXCLUDED.updated_at
                     """)) {
            Timestamp now = Timestamp.from(Instant.now());
            statement.setString(1, tenantId);
            statement.setString(2, integrationName);
            statement.setTimestamp(3, now);
            statement.setTimestamp(4, now);
            statement.setInt(5, responseTimeMs);
            statement.setTimestamp(6, now);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not apply api integration success", exception);
        }
    }

    @Override
    public int applyFailure(String tenantId, String integrationName, Integer responseTimeMs,
                             ApiFailureType failureType, ApiSeverity severity) {
        Timestamp now = Timestamp.from(Instant.now());
        IntegrationHealthStatus initialStatus = severity == ApiSeverity.CRITICAL
                ? IntegrationHealthStatus.DOWN
                : IntegrationHealthStatus.DEGRADED;

        try (Connection connection = dataSource.getConnection()) {
            int consecutiveFailures;
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO api_integration_status
                    (tenant_id, integration_name, current_status, last_check_at, last_failure_at,
                     last_failure_type, consecutive_failures, avg_response_time_ms, updated_at)
                    VALUES (?, ?, ?, ?, ?, ?, 1, ?, ?)
                    ON CONFLICT (tenant_id, integration_name) DO UPDATE SET
                        last_check_at = EXCLUDED.last_check_at,
                        last_failure_at = EXCLUDED.last_failure_at,
                        last_failure_type = EXCLUDED.last_failure_type,
                        consecutive_failures = api_integration_status.consecutive_failures + 1,
                        avg_response_time_ms = CASE
                            WHEN EXCLUDED.avg_response_time_ms IS NULL THEN api_integration_status.avg_response_time_ms
                            WHEN api_integration_status.avg_response_time_ms IS NULL THEN EXCLUDED.avg_response_time_ms
                            ELSE (api_integration_status.avg_response_time_ms + EXCLUDED.avg_response_time_ms) / 2
                        END,
                        updated_at = EXCLUDED.updated_at
                    RETURNING consecutive_failures
                    """)) {
                statement.setString(1, tenantId);
                statement.setString(2, integrationName);
                statement.setString(3, initialStatus.name());
                statement.setTimestamp(4, now);
                statement.setTimestamp(5, now);
                statement.setString(6, failureType == null ? null : failureType.name());
                setNullableInt(statement, 7, responseTimeMs);
                statement.setTimestamp(8, now);
                try (ResultSet resultSet = statement.executeQuery()) {
                    resultSet.next();
                    consecutiveFailures = resultSet.getInt("consecutive_failures");
                }
            }

            IntegrationHealthStatus finalStatus = severity == ApiSeverity.CRITICAL
                    || consecutiveFailures >= DOWN_AFTER_CONSECUTIVE_FAILURES
                    ? IntegrationHealthStatus.DOWN
                    : IntegrationHealthStatus.DEGRADED;

            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE api_integration_status
                    SET current_status = ?
                    WHERE tenant_id = ? AND integration_name = ?
                    """)) {
                statement.setString(1, finalStatus.name());
                statement.setString(2, tenantId);
                statement.setString(3, integrationName);
                statement.executeUpdate();
            }

            return consecutiveFailures;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not apply api integration failure", exception);
        }
    }

    @Override
    public List<IntegrationHealthSummary> findHealthSummaries(String tenantId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT integration_name, current_status, last_check_at, last_success_at,
                            last_failure_at, last_failure_type, avg_response_time_ms,
                            requests_24h, failures_24h, availability_pct_24h
                     FROM api_integration_status
                     WHERE tenant_id = ?
                     """)) {
            statement.setString(1, tenantId);
            List<IntegrationHealthSummary> summaries = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    summaries.add(readSummary(resultSet));
                }
            }
            return summaries;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find api integration health summaries", exception);
        }
    }

    @Override
    public void recompute24hWindow(String tenantId, String integrationName, int requests24h, int failures24h, BigDecimal availabilityPct24h) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE api_integration_status
                     SET requests_24h = ?,
                         failures_24h = ?,
                         availability_pct_24h = ?,
                         updated_at = ?
                     WHERE tenant_id = ? AND integration_name = ?
                     """)) {
            statement.setInt(1, requests24h);
            statement.setInt(2, failures24h);
            if (availabilityPct24h == null) {
                statement.setNull(3, Types.NUMERIC);
            } else {
                statement.setBigDecimal(3, availabilityPct24h);
            }
            statement.setTimestamp(4, Timestamp.from(Instant.now()));
            statement.setString(5, tenantId);
            statement.setString(6, integrationName);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not recompute api integration 24h window", exception);
        }
    }

    @Override
    public List<String[]> findAllTenantIntegrationPairs() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT DISTINCT tenant_id, integration_name
                     FROM api_integration_status
                     """)) {
            List<String[]> pairs = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    pairs.add(new String[] {resultSet.getString("tenant_id"), resultSet.getString("integration_name")});
                }
            }
            return pairs;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find tenant integration pairs", exception);
        }
    }

    private IntegrationHealthSummary readSummary(ResultSet resultSet) throws SQLException {
        return new IntegrationHealthSummary(
                resultSet.getString("integration_name"),
                IntegrationHealthStatus.valueOf(resultSet.getString("current_status")),
                instant(resultSet, "last_check_at"),
                instant(resultSet, "last_success_at"),
                instant(resultSet, "last_failure_at"),
                nullableEnum(resultSet, "last_failure_type", ApiFailureType.class),
                integer(resultSet, "avg_response_time_ms"),
                resultSet.getInt("requests_24h"),
                resultSet.getInt("failures_24h"),
                resultSet.getBigDecimal("availability_pct_24h")
        );
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Integer integer(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private <T extends Enum<T>> T nullableEnum(ResultSet resultSet, String columnName, Class<T> enumType) throws SQLException {
        String value = resultSet.getString(columnName);
        return value == null ? null : Enum.valueOf(enumType, value);
    }
}
