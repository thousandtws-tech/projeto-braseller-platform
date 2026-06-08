package com.example.infrastructure.persistence;

import com.example.application.event.SyncAllRequestedEvent;
import com.example.application.port.out.ConnectorSyncJobRepository;
import com.example.domain.enums.SyncJobStatus;
import com.example.domain.model.connector.SyncJob;
import com.example.domain.model.connector.SyncResult;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

@ApplicationScoped
public class JdbcConnectorSyncJobRepository implements ConnectorSyncJobRepository {
    private static final long STALE_PROCESSING_TIMEOUT_SECONDS = 900;

    @Inject
    DataSource dataSource;

    @Override
    public SyncJob createQueued(SyncAllRequestedEvent event) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    INSERT INTO connector_sync_jobs
                    (id, tenant_id, connector_name, since_instant, status,
                     recipient_email, requested_at)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    """)) {
                statement.setString(1, event.eventId());
                statement.setString(2, event.tenantId());
                statement.setString(3, event.connectorName());
                statement.setTimestamp(4, Timestamp.from(event.since()));
                statement.setString(5, SyncJobStatus.QUEUED.name());
                statement.setString(6, event.recipientEmail());
                statement.setTimestamp(7, Timestamp.from(event.requestedAt()));
                statement.executeUpdate();
            }
            return find(connection, event.tenantId(), event.eventId())
                    .orElseThrow(() -> new RepositoryException("Could not find queued sync job", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not create connector sync job", exception);
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
                     UPDATE connector_sync_jobs
                     SET status = ?,
                         started_at = ?,
                         error_message = NULL
                     WHERE id = ?
                       AND (status = ?
                         OR (status = ? AND started_at < ?))
                     """)) {
            Timestamp now = Timestamp.from(Instant.now());
            Timestamp staleBefore = Timestamp.from(Instant.now().minusSeconds(STALE_PROCESSING_TIMEOUT_SECONDS));
            statement.setString(1, SyncJobStatus.PROCESSING.name());
            statement.setTimestamp(2, now);
            statement.setString(3, jobId);
            statement.setString(4, SyncJobStatus.QUEUED.name());
            statement.setString(5, SyncJobStatus.PROCESSING.name());
            statement.setTimestamp(6, staleBefore);
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not mark connector sync job as processing", exception);
        }
    }

    @Override
    public SyncJob markCompleted(String jobId, SyncResult result) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE connector_sync_jobs
                    SET status = ?,
                        finished_at = ?,
                        error_message = NULL,
                        orders_synced = ?,
                        payments_synced = ?,
                        fees_synced = ?
                    WHERE id = ?
                    """)) {
                statement.setString(1, SyncJobStatus.COMPLETED.name());
                statement.setTimestamp(2, Timestamp.from(Instant.now()));
                statement.setInt(3, result.ordersSynced());
                statement.setInt(4, result.paymentsSynced());
                statement.setInt(5, result.feesSynced());
                statement.setString(6, jobId);
                statement.executeUpdate();
            }
            return findById(connection, jobId)
                    .orElseThrow(() -> new RepositoryException("Could not find completed sync job", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not mark connector sync job as completed", exception);
        }
    }

    @Override
    public SyncJob markFailed(String jobId, String errorMessage) {
        try (Connection connection = dataSource.getConnection()) {
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE connector_sync_jobs
                    SET status = ?,
                        finished_at = ?,
                        error_message = ?
                    WHERE id = ?
                    """)) {
                statement.setString(1, SyncJobStatus.FAILED.name());
                statement.setTimestamp(2, Timestamp.from(Instant.now()));
                statement.setString(3, truncate(errorMessage, 600));
                statement.setString(4, jobId);
                statement.executeUpdate();
            }
            return findById(connection, jobId)
                    .orElseThrow(() -> new RepositoryException("Could not find failed sync job", null));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not mark connector sync job as failed", exception);
        }
    }

    @Override
    public Optional<SyncJob> find(String tenantId, String jobId) {
        try (Connection connection = dataSource.getConnection()) {
            return find(connection, tenantId, jobId);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find connector sync job", exception);
        }
    }

    private Optional<SyncJob> find(Connection connection, String tenantId, String jobId) throws SQLException {
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

    private Optional<SyncJob> findById(Connection connection, String jobId) throws SQLException {
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
                SELECT id, tenant_id, connector_name, since_instant, status,
                       recipient_email, requested_at, started_at, finished_at,
                       error_message, orders_synced, payments_synced, fees_synced
                FROM connector_sync_jobs
                """;
    }

    private SyncJob readJob(ResultSet resultSet) throws SQLException {
        return new SyncJob(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("connector_name"),
                resultSet.getTimestamp("since_instant").toInstant(),
                SyncJobStatus.valueOf(resultSet.getString("status")),
                resultSet.getString("recipient_email"),
                resultSet.getTimestamp("requested_at").toInstant(),
                instant(resultSet, "started_at"),
                instant(resultSet, "finished_at"),
                resultSet.getString("error_message"),
                integer(resultSet, "orders_synced"),
                integer(resultSet, "payments_synced"),
                integer(resultSet, "fees_synced")
        );
    }

    private Instant instant(ResultSet resultSet, String columnName) throws SQLException {
        Timestamp timestamp = resultSet.getTimestamp(columnName);
        return timestamp == null ? null : timestamp.toInstant();
    }

    private Integer integer(ResultSet resultSet, String columnName) throws SQLException {
        int value = resultSet.getInt(columnName);
        return resultSet.wasNull() ? null : value;
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
