package com.example.infrastructure.persistence;

import com.example.application.port.out.OutboxEventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class JdbcOutboxEventRepository implements OutboxEventRepository {

    @Inject
    DataSource dataSource;

    @Override
    public void save(String aggregateType, String aggregateId, String eventType, String payloadJson) {
        String sql = """
                INSERT INTO outbox_events (id, aggregate_type, aggregate_id, event_type, payload_json, created_at)
                VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, aggregateType);
            ps.setString(3, aggregateId);
            ps.setString(4, eventType);
            ps.setString(5, payloadJson);
            ps.setTimestamp(6, Timestamp.from(Instant.now()));
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to save outbox event", e);
        }
    }

    @Override
    public List<PendingEvent> findPending(int limit) {
        String sql = "SELECT * FROM outbox_events WHERE processed_at IS NULL AND failed_at IS NULL ORDER BY created_at ASC LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setInt(1, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<PendingEvent> result = new ArrayList<>();
                while (rs.next()) {
                    result.add(new PendingEvent(
                            rs.getString("id"),
                            rs.getString("aggregate_type"),
                            rs.getString("aggregate_id"),
                            rs.getString("event_type"),
                            rs.getString("payload_json")
                    ));
                }
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find pending events", e);
        }
    }

    @Override
    public void markProcessed(String id) {
        String sql = "UPDATE outbox_events SET processed_at = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to mark event as processed", e);
        }
    }

    @Override
    public void markFailed(String id, String error) {
        String sql = "UPDATE outbox_events SET failed_at = ?, error = ? WHERE id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setTimestamp(1, Timestamp.from(Instant.now()));
            ps.setString(2, error);
            ps.setString(3, id);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to mark event as failed", e);
        }
    }
}
