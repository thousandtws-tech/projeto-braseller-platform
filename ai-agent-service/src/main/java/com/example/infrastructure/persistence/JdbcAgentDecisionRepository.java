package com.example.infrastructure.persistence;

import com.example.application.port.out.AgentDecisionRepository;
import com.example.domain.model.AgentDecision;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcAgentDecisionRepository implements AgentDecisionRepository {

    @Inject
    DataSource dataSource;

    @Override
    public AgentDecision save(AgentDecision d) {
        String sql = """
                INSERT INTO agent_decisions
                (id, tenant_id, agent_id, execution_id, goal_id, context_json, reasoning,
                 decision, confidence, selected_tool, tool_input_json, outcome, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, d.id());
            ps.setString(2, d.tenantId());
            ps.setString(3, d.agentId());
            ps.setString(4, d.executionId());
            ps.setString(5, d.goalId());
            ps.setString(6, d.contextJson());
            ps.setString(7, d.reasoning());
            ps.setString(8, d.decision());
            ps.setDouble(9, d.confidence());
            ps.setString(10, d.selectedTool());
            ps.setString(11, d.toolInputJson());
            ps.setString(12, d.outcome());
            ps.setTimestamp(13, Timestamp.from(d.createdAt()));
            ps.executeUpdate();
            return d;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to save decision", e);
        }
    }

    @Override
    public Optional<AgentDecision> findById(String id, String tenantId) {
        String sql = "SELECT * FROM agent_decisions WHERE id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find decision", e);
        }
    }

    @Override
    public List<AgentDecision> findByExecutionId(String executionId, String tenantId) {
        String sql = "SELECT * FROM agent_decisions WHERE execution_id = ? AND tenant_id = ? ORDER BY created_at ASC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, executionId);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AgentDecision> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to list decisions by execution", e);
        }
    }

    @Override
    public List<AgentDecision> findRecentByAgent(String agentId, String tenantId, int limit) {
        String sql = "SELECT * FROM agent_decisions WHERE agent_id = ? AND tenant_id = ? ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<AgentDecision> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to list recent decisions", e);
        }
    }

    @Override
    public AgentDecision updateOutcome(String id, String tenantId, String outcome) {
        String sql = "UPDATE agent_decisions SET outcome = ? WHERE id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, outcome);
            ps.setString(2, id);
            ps.setString(3, tenantId);
            ps.executeUpdate();
            return findById(id, tenantId).orElseThrow();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to update decision outcome", e);
        }
    }

    private AgentDecision map(ResultSet rs) throws SQLException {
        return new AgentDecision(
                rs.getString("id"), rs.getString("tenant_id"), rs.getString("agent_id"),
                rs.getString("execution_id"), rs.getString("goal_id"),
                rs.getString("context_json"), rs.getString("reasoning"),
                rs.getString("decision"), rs.getDouble("confidence"),
                rs.getString("selected_tool"), rs.getString("tool_input_json"),
                rs.getString("outcome"), rs.getTimestamp("created_at").toInstant()
        );
    }
}
