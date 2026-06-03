package com.example.infrastructure.persistence;

import com.example.application.port.out.AgentExecutionRepository;
import com.example.domain.model.AgentExecution;
import com.example.domain.model.ExecutionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcAgentExecutionRepository implements AgentExecutionRepository {

    @Inject
    DataSource dataSource;

    @Override
    public AgentExecution save(AgentExecution execution) {
        String sql = """
                INSERT INTO agent_executions
                    (id, tenant_id, agent_id, goal_id, triggered_by, status,
                     total_actions, success_actions, failed_actions, summary, error, started_at, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, execution.id());
            ps.setString(2, execution.tenantId());
            ps.setString(3, execution.agentId());
            ps.setString(4, execution.goalId());
            ps.setString(5, execution.triggeredBy());
            ps.setString(6, execution.status().name());
            ps.setInt(7, execution.totalActions());
            ps.setInt(8, execution.successActions());
            ps.setInt(9, execution.failedActions());
            ps.setString(10, execution.summary());
            ps.setString(11, execution.error());
            ps.setTimestamp(12, Timestamp.from(execution.startedAt()));
            ps.setTimestamp(13, Timestamp.from(execution.createdAt()));
            ps.executeUpdate();
            return execution;
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to save execution", ex);
        }
    }

    @Override
    public Optional<AgentExecution> findById(String id, String tenantId) {
        String sql = "SELECT * FROM agent_executions WHERE id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to find execution", ex);
        }
    }

    @Override
    public List<AgentExecution> findByAgentId(String agentId, String tenantId, int limit) {
        String sql = "SELECT * FROM agent_executions WHERE agent_id = ? AND tenant_id = ? ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<AgentExecution> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to list executions", ex);
        }
    }

    @Override
    public boolean hasRunningExecution(String agentId, String tenantId) {
        String sql = "SELECT COUNT(1) FROM agent_executions WHERE agent_id = ? AND tenant_id = ? AND status = 'RUNNING'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() && rs.getInt(1) > 0;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to check running execution", ex);
        }
    }

    @Override
    public AgentExecution finish(String id, String tenantId, ExecutionStatus status,
                                 int totalActions, int successActions, int failedActions,
                                 String summary, String error, Instant finishedAt) {
        String sql = """
                UPDATE agent_executions SET status = ?, total_actions = ?, success_actions = ?,
                    failed_actions = ?, summary = ?, error = ?, finished_at = ?
                WHERE id = ? AND tenant_id = ?
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setInt(2, totalActions);
            ps.setInt(3, successActions);
            ps.setInt(4, failedActions);
            ps.setString(5, summary);
            ps.setString(6, error);
            ps.setTimestamp(7, Timestamp.from(finishedAt));
            ps.setString(8, id);
            ps.setString(9, tenantId);
            ps.executeUpdate();
            return findById(id, tenantId).orElseThrow();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to finish execution", ex);
        }
    }

    private AgentExecution map(ResultSet rs) throws SQLException {
        Timestamp finishedAt = rs.getTimestamp("finished_at");
        return new AgentExecution(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("agent_id"),
                rs.getString("goal_id"),
                rs.getString("triggered_by"),
                ExecutionStatus.valueOf(rs.getString("status")),
                rs.getInt("total_actions"),
                rs.getInt("success_actions"),
                rs.getInt("failed_actions"),
                rs.getString("summary"),
                rs.getString("error"),
                rs.getTimestamp("started_at").toInstant(),
                finishedAt != null ? finishedAt.toInstant() : null,
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
