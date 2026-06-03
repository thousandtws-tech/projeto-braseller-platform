package com.example.infrastructure.persistence;

import com.example.application.port.out.AgentGoalRepository;
import com.example.domain.model.AgentGoal;
import com.example.domain.model.GoalStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcAgentGoalRepository implements AgentGoalRepository {

    @Inject
    DataSource dataSource;

    @Override
    public AgentGoal save(AgentGoal goal) {
        String sql = """
                INSERT INTO agent_goals (id, tenant_id, agent_id, title, description, objective,
                    priority, status, deadline, result, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, goal.id());
            ps.setString(2, goal.tenantId());
            ps.setString(3, goal.agentId());
            ps.setString(4, goal.title());
            ps.setString(5, goal.description());
            ps.setString(6, goal.objective());
            ps.setInt(7, goal.priority());
            ps.setString(8, goal.status().name());
            ps.setTimestamp(9, goal.deadline() != null ? Timestamp.from(goal.deadline()) : null);
            ps.setString(10, goal.result());
            ps.setTimestamp(11, Timestamp.from(goal.createdAt()));
            ps.setTimestamp(12, Timestamp.from(goal.updatedAt()));
            ps.executeUpdate();
            return goal;
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to save goal", ex);
        }
    }

    @Override
    public Optional<AgentGoal> findById(String id, String tenantId) {
        String sql = "SELECT * FROM agent_goals WHERE id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to find goal", ex);
        }
    }

    @Override
    public List<AgentGoal> findByAgentId(String agentId, String tenantId) {
        String sql = "SELECT * FROM agent_goals WHERE agent_id = ? AND tenant_id = ? ORDER BY priority DESC, created_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AgentGoal> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to list goals", ex);
        }
    }

    @Override
    public List<AgentGoal> findPendingByTenantId(String tenantId) {
        String sql = "SELECT * FROM agent_goals WHERE tenant_id = ? AND status = 'PENDING' ORDER BY priority DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AgentGoal> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to find pending goals", ex);
        }
    }

    @Override
    public AgentGoal updateStatus(String id, String tenantId, GoalStatus status, String result) {
        String sql = "UPDATE agent_goals SET status = ?, result = ?, updated_at = ? WHERE id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setString(2, result);
            ps.setTimestamp(3, Timestamp.from(Instant.now()));
            ps.setString(4, id);
            ps.setString(5, tenantId);
            ps.executeUpdate();
            return findById(id, tenantId).orElseThrow();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to update goal status", ex);
        }
    }

    private AgentGoal map(ResultSet rs) throws SQLException {
        Timestamp deadline = rs.getTimestamp("deadline");
        return new AgentGoal(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("agent_id"),
                rs.getString("title"),
                rs.getString("description"),
                rs.getString("objective"),
                rs.getInt("priority"),
                GoalStatus.valueOf(rs.getString("status")),
                deadline != null ? deadline.toInstant() : null,
                rs.getString("result"),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
