package com.example.infrastructure.messaging;

import com.example.infrastructure.persistence.RepositoryException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class JdbcOutboxEventRepository {
    private static final String PENDING = "PENDING";
    private static final String FAILED = "FAILED";
    private static final String PUBLISHING = "PUBLISHING";
    private static final String PUBLISHED = "PUBLISHED";
    private static final String DEAD = "DEAD";

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "messaging.outbox.in-flight-timeout-seconds", defaultValue = "300")
    long inFlightTimeoutSeconds;

    public void save(String id, String eventType, String aggregateId, String partitionKey, Object payload) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO messaging_outbox_events
                     (id, event_type, aggregate_id, partition_key, payload, status,
                      attempts, next_attempt_at, created_at, updated_at)
                     VALUES (?, ?, ?, ?, ?, ?, 0, ?, ?, ?)
                     """)) {
            Timestamp now = Timestamp.from(Instant.now());
            statement.setString(1, id);
            statement.setString(2, eventType);
            statement.setString(3, aggregateId);
            statement.setString(4, partitionKey);
            statement.setString(5, objectMapper.writeValueAsString(payload));
            statement.setString(6, PENDING);
            statement.setTimestamp(7, now);
            statement.setTimestamp(8, now);
            statement.setTimestamp(9, now);
            statement.executeUpdate();
        } catch (JsonProcessingException exception) {
            throw new RepositoryException("Could not serialize outbox event", exception);
        } catch (SQLException exception) {
            if (isDuplicate(exception)) {
                return;
            }
            throw new RepositoryException("Could not save outbox event", exception);
        }
    }

    public List<OutboxEvent> findReady(int batchSize) {
        Instant now = Instant.now();
        Instant stale = now.minusSeconds(inFlightTimeoutSeconds);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, event_type, aggregate_id, partition_key, payload, attempts
                     FROM messaging_outbox_events
                     WHERE ((status IN (?, ?) AND next_attempt_at <= ?)
                         OR (status = ? AND updated_at <= ?))
                     ORDER BY created_at ASC
                     LIMIT ?
                     """)) {
            statement.setString(1, PENDING);
            statement.setString(2, FAILED);
            statement.setTimestamp(3, Timestamp.from(now));
            statement.setString(4, PUBLISHING);
            statement.setTimestamp(5, Timestamp.from(stale));
            statement.setInt(6, batchSize);
            List<OutboxEvent> events = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    events.add(new OutboxEvent(
                            resultSet.getString("id"),
                            resultSet.getString("event_type"),
                            resultSet.getString("aggregate_id"),
                            resultSet.getString("partition_key"),
                            resultSet.getString("payload"),
                            resultSet.getInt("attempts")
                    ));
                }
            }
            return events;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find outbox events", exception);
        }
    }

    public boolean markPublishing(String id) {
        Instant now = Instant.now();
        Instant stale = now.minusSeconds(inFlightTimeoutSeconds);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE messaging_outbox_events
                     SET status = ?,
                         attempts = attempts + 1,
                         updated_at = ?,
                         last_error = NULL
                     WHERE id = ?
                       AND ((status IN (?, ?) AND next_attempt_at <= ?)
                         OR (status = ? AND updated_at <= ?))
                     """)) {
            statement.setString(1, PUBLISHING);
            statement.setTimestamp(2, Timestamp.from(now));
            statement.setString(3, id);
            statement.setString(4, PENDING);
            statement.setString(5, FAILED);
            statement.setTimestamp(6, Timestamp.from(now));
            statement.setString(7, PUBLISHING);
            statement.setTimestamp(8, Timestamp.from(stale));
            return statement.executeUpdate() == 1;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not mark outbox event as publishing", exception);
        }
    }

    public void markPublished(String id) {
        Instant now = Instant.now();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE messaging_outbox_events
                     SET status = ?,
                         published_at = ?,
                         updated_at = ?,
                         last_error = NULL
                     WHERE id = ?
                     """)) {
            statement.setString(1, PUBLISHED);
            statement.setTimestamp(2, Timestamp.from(now));
            statement.setTimestamp(3, Timestamp.from(now));
            statement.setString(4, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not mark outbox event as published", exception);
        }
    }

    public void markFailed(String id, String errorMessage, int maxAttempts, long retryDelaySeconds) {
        Instant now = Instant.now();
        Instant nextAttempt = now.plusSeconds(retryDelaySeconds);
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE messaging_outbox_events
                     SET status = CASE WHEN attempts >= ? THEN ? ELSE ? END,
                         next_attempt_at = ?,
                         updated_at = ?,
                         last_error = ?
                     WHERE id = ?
                     """)) {
            statement.setInt(1, maxAttempts);
            statement.setString(2, DEAD);
            statement.setString(3, FAILED);
            statement.setTimestamp(4, Timestamp.from(nextAttempt));
            statement.setTimestamp(5, Timestamp.from(now));
            statement.setString(6, truncate(errorMessage, 600));
            statement.setString(7, id);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not mark outbox event as failed", exception);
        }
    }

    private boolean isDuplicate(SQLException exception) {
        String sqlState = exception.getSQLState();
        return sqlState != null && sqlState.startsWith("23");
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
