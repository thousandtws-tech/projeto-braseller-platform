package com.example.infrastructure.persistence;

import com.example.application.port.out.ApiIntegrationEventRepository;
import com.example.domain.enums.ApiCallOutcome;
import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;
import com.example.domain.model.monitoring.IntegrationEventLog;
import com.example.domain.model.monitoring.NewApiIntegrationEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.sql.Types;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class JdbcApiIntegrationEventRepository implements ApiIntegrationEventRepository {
    private static final int MAX_LIMIT = 500;

    @Inject
    DataSource dataSource;

    @Override
    public void record(NewApiIntegrationEvent event) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO api_integration_events
                     (id, tenant_id, integration_name, endpoint, operation, occurred_at,
                      response_time_ms, http_status, outcome, failure_type, severity,
                      impact, action_taken, error_message)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, UUID.randomUUID().toString());
            statement.setString(2, event.tenantId());
            statement.setString(3, event.integrationName());
            statement.setString(4, event.endpoint());
            statement.setString(5, event.operation());
            statement.setTimestamp(6, Timestamp.from(event.occurredAt()));
            setNullableInt(statement, 7, event.responseTimeMs());
            setNullableInt(statement, 8, event.httpStatus());
            statement.setString(9, event.outcome().name());
            statement.setString(10, event.failureType() == null ? null : event.failureType().name());
            statement.setString(11, event.severity().name());
            statement.setString(12, truncate(event.impact(), 400));
            statement.setString(13, truncate(event.actionTaken(), 400));
            statement.setString(14, truncate(event.errorMessage(), 1000));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not record api integration event", exception);
        }
    }

    @Override
    public List<IntegrationEventLog> findLogs(String tenantId, String integrationName, ApiSeverity severity, int limit) {
        int effectiveLimit = limit <= 0 ? 100 : Math.min(limit, MAX_LIMIT);
        StringBuilder sql = new StringBuilder("""
                SELECT id, integration_name, occurred_at, endpoint, operation, response_time_ms,
                       http_status, outcome, failure_type, severity, impact, action_taken, error_message
                FROM api_integration_events
                WHERE tenant_id = ?
                """);
        if (integrationName != null) {
            sql.append(" AND integration_name = ?");
        }
        if (severity != null) {
            sql.append(" AND severity = ?");
        }
        sql.append(" ORDER BY occurred_at DESC LIMIT ?");

        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            int index = 1;
            statement.setString(index++, tenantId);
            if (integrationName != null) {
                statement.setString(index++, integrationName);
            }
            if (severity != null) {
                statement.setString(index++, severity.name());
            }
            statement.setInt(index, effectiveLimit);

            List<IntegrationEventLog> logs = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    logs.add(readLog(resultSet));
                }
            }
            return logs;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find api integration logs", exception);
        }
    }

    @Override
    public int countSince(String tenantId, String integrationName, Instant since) {
        return countSince(tenantId, integrationName, since, null);
    }

    @Override
    public int countFailuresSince(String tenantId, String integrationName, Instant since) {
        return countSince(tenantId, integrationName, since, ApiCallOutcome.FAILURE);
    }

    private int countSince(String tenantId, String integrationName, Instant since, ApiCallOutcome outcome) {
        StringBuilder sql = new StringBuilder("""
                SELECT COUNT(*) AS total
                FROM api_integration_events
                WHERE tenant_id = ? AND integration_name = ? AND occurred_at >= ?
                """);
        if (outcome != null) {
            sql.append(" AND outcome = ?");
        }
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement(sql.toString())) {
            statement.setString(1, tenantId);
            statement.setString(2, integrationName);
            statement.setTimestamp(3, Timestamp.from(since));
            if (outcome != null) {
                statement.setString(4, outcome.name());
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getInt("total") : 0;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not count api integration events", exception);
        }
    }

    private IntegrationEventLog readLog(ResultSet resultSet) throws SQLException {
        return new IntegrationEventLog(
                resultSet.getString("id"),
                resultSet.getString("integration_name"),
                resultSet.getTimestamp("occurred_at").toInstant(),
                resultSet.getString("endpoint"),
                resultSet.getString("operation"),
                nullableInt(resultSet, "response_time_ms"),
                nullableInt(resultSet, "http_status"),
                ApiCallOutcome.valueOf(resultSet.getString("outcome")),
                nullableEnum(resultSet, "failure_type", ApiFailureType.class),
                ApiSeverity.valueOf(resultSet.getString("severity")),
                resultSet.getString("impact"),
                resultSet.getString("action_taken"),
                resultSet.getString("error_message")
        );
    }

    private void setNullableInt(PreparedStatement statement, int index, Integer value) throws SQLException {
        if (value == null) {
            statement.setNull(index, Types.INTEGER);
        } else {
            statement.setInt(index, value);
        }
    }

    private Integer nullableInt(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private <T extends Enum<T>> T nullableEnum(ResultSet resultSet, String columnName, Class<T> enumType) throws SQLException {
        String value = resultSet.getString(columnName);
        return value == null ? null : Enum.valueOf(enumType, value);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
