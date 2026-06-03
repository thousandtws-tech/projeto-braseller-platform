package com.example.infrastructure.persistence;

import com.example.application.port.out.AgentActionRepository;
import com.example.domain.model.AgentAction;
import com.example.domain.model.ActionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcAgentActionRepository implements AgentActionRepository {

    @Inject
    DataSource dataSource;

    @Override
    public AgentAction save(AgentAction a) {
        String sql = """
                INSERT INTO agent_actions
                (id, tenant_id, agent_id, execution_id, task_id, action_type, tool_name,
                 input_json, output_json, status, error, duration_ms, started_at, finished_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, a.id());
            ps.setString(2, a.tenantId());
            ps.setString(3, a.agentId());
            ps.setString(4, a.executionId());
            ps.setString(5, a.taskId());
            ps.setString(6, a.actionType());
            ps.setString(7, a.toolName());
            ps.setString(8, a.inputJson());
            ps.setString(9, a.outputJson());
            ps.setString(10, a.status().name());
            ps.setString(11, a.error());
            ps.setLong(12, a.durationMs());
            ps.setTimestamp(13, a.startedAt() != null ? Timestamp.from(a.startedAt()) : null);
            ps.setTimestamp(14, a.finishedAt() != null ? Timestamp.from(a.finishedAt()) : null);
            ps.setTimestamp(15, Timestamp.from(a.createdAt()));
            ps.executeUpdate();
            return a;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to save action", e);
        }
    }

    @Override
    public Optional<AgentAction> findById(String id, String tenantId) {
        String sql = "SELECT * FROM agent_actions WHERE id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find action", e);
        }
    }

    @Override
    public List<AgentAction> findByExecutionId(String executionId, String tenantId) {
        String sql = "SELECT * FROM agent_actions WHERE execution_id = ? AND tenant_id = ? ORDER BY created_at ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, executionId);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AgentAction> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to list actions", e);
        }
    }

    @Override
    public AgentAction finish(String id, String tenantId, ActionStatus status,
                              String outputJson, String error, long durationMs, Instant finishedAt) {
        String sql = """
                UPDATE agent_actions SET status = ?, output_json = ?, error = ?, duration_ms = ?, finished_at = ?
                WHERE id = ? AND tenant_id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, outputJson);
            ps.setString(3, error);
            ps.setLong(4, durationMs);
            ps.setTimestamp(5, Timestamp.from(finishedAt));
            ps.setString(6, id);
            ps.setString(7, tenantId);
            ps.executeUpdate();
            return findById(id, tenantId).orElseThrow();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to finish action", e);
        }
    }

    private AgentAction map(ResultSet rs) throws SQLException {
        Timestamp startedAt = rs.getTimestamp("started_at");
        Timestamp finishedAt = rs.getTimestamp("finished_at");
        return new AgentAction(
                rs.getString("id"), rs.getString("tenant_id"), rs.getString("agent_id"),
                rs.getString("execution_id"), rs.getString("task_id"),
                rs.getString("action_type"), rs.getString("tool_name"),
                rs.getString("input_json"), rs.getString("output_json"),
                ActionStatus.valueOf(rs.getString("status")),
                rs.getString("error"), rs.getLong("duration_ms"),
                startedAt != null ? startedAt.toInstant() : null,
                finishedAt != null ? finishedAt.toInstant() : null,
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
