package com.example.infrastructure.persistence;

import com.example.application.port.out.ConnectorRealtimeEventStore;
import com.example.domain.model.connector.ConnectorRealtimeEvent;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@ApplicationScoped
public class JdbcConnectorRealtimeEventRepository implements ConnectorRealtimeEventStore {
    private static final TypeReference<Map<String, Object>> PAYLOAD_TYPE = new TypeReference<>() {
    };

    @Inject
    DataSource dataSource;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public ConnectorRealtimeEvent append(
            String tenantId,
            String eventType,
            String aggregateId,
            Object payload) {
        try (Connection connection = dataSource.getConnection()) {
            return append(connection, tenantId, eventType, aggregateId, payload);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not append connector realtime event", exception);
        }
    }

    public ConnectorRealtimeEvent append(
            Connection connection,
            String tenantId,
            String eventType,
            String aggregateId,
            Object payload) throws SQLException {
        String eventId = UUID.randomUUID().toString();
        Instant occurredAt = Instant.now();
        String serializedPayload = serialize(payload);

        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO connector_realtime_events
                (event_id, tenant_id, event_type, aggregate_id, occurred_at, payload)
                VALUES (?, ?, ?, ?, ?, ?)
                """, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, eventId);
            statement.setString(2, tenantId);
            statement.setString(3, eventType);
            statement.setString(4, aggregateId);
            statement.setTimestamp(5, Timestamp.from(occurredAt));
            statement.setString(6, serializedPayload);
            statement.executeUpdate();

            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (!keys.next()) {
                    throw new SQLException("Realtime event sequence was not generated");
                }
                return new ConnectorRealtimeEvent(
                        keys.getLong(1),
                        eventId,
                        eventType,
                        aggregateId,
                        occurredAt,
                        deserialize(serializedPayload)
                );
            }
        }
    }

    @Override
    public List<ConnectorRealtimeEvent> findAfter(String tenantId, long cursor, int limit) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT sequence_id, event_id, event_type, aggregate_id, occurred_at, payload
                     FROM connector_realtime_events
                     WHERE tenant_id = ? AND sequence_id > ?
                     ORDER BY sequence_id
                     LIMIT ?
                     """)) {
            statement.setString(1, tenantId);
            statement.setLong(2, Math.max(0, cursor));
            statement.setInt(3, Math.max(1, Math.min(limit, 1000)));
            try (ResultSet resultSet = statement.executeQuery()) {
                var events = new java.util.ArrayList<ConnectorRealtimeEvent>();
                while (resultSet.next()) {
                    events.add(readEvent(resultSet));
                }
                return List.copyOf(events);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not read connector realtime events", exception);
        }
    }

    @Override
    public int deleteOlderThan(Instant cutoff) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM connector_realtime_events
                     WHERE occurred_at < ?
                     """)) {
            statement.setTimestamp(1, Timestamp.from(cutoff));
            return statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not clean connector realtime events", exception);
        }
    }

    private ConnectorRealtimeEvent readEvent(ResultSet resultSet) throws SQLException {
        return new ConnectorRealtimeEvent(
                resultSet.getLong("sequence_id"),
                resultSet.getString("event_id"),
                resultSet.getString("event_type"),
                resultSet.getString("aggregate_id"),
                resultSet.getTimestamp("occurred_at").toInstant(),
                deserialize(resultSet.getString("payload"))
        );
    }

    private String serialize(Object payload) {
        try {
            return objectMapper.writeValueAsString(payload);
        } catch (Exception exception) {
            throw new RepositoryException("Could not serialize connector realtime event", exception);
        }
    }

    private Map<String, Object> deserialize(String payload) {
        try {
            return objectMapper.readValue(payload, PAYLOAD_TYPE);
        } catch (Exception exception) {
            throw new RepositoryException("Could not deserialize connector realtime event", exception);
        }
    }
}
