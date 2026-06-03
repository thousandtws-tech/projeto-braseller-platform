package com.example.infrastructure.persistence;

import com.example.application.port.out.AgentRepository;
import com.example.domain.model.Agent;
import com.example.domain.model.AgentStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcAgentRepository implements AgentRepository {

    @Inject
    DataSource dataSource;

    @Override
    public Agent save(Agent agent) {
        String sql = """
                INSERT INTO agents (id, tenant_id, name, description, agent_type, capabilities, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agent.id());
            ps.setString(2, agent.tenantId());
            ps.setString(3, agent.name());
            ps.setString(4, agent.description());
            ps.setString(5, agent.agentType());
            ps.setString(6, agent.capabilities());
            ps.setString(7, agent.status().name());
            ps.setTimestamp(8, Timestamp.from(agent.createdAt()));
            ps.setTimestamp(9, Timestamp.from(agent.updatedAt()));
            ps.executeUpdate();
            return agent;
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to save agent", ex);
        }
    }

    @Override
    public Optional<Agent> findById(String id, String tenantId) {
        String sql = "SELECT * FROM agents WHERE id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(map(rs));
                }
                return Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to find agent by id", ex);
        }
    }

    @Override
    public List<Agent> findByTenantId(String tenantId) {
        String sql = "SELECT * FROM agents WHERE tenant_id = ? ORDER BY created_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<Agent> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to list agents", ex);
        }
    }

    @Override
    public Agent updateStatus(String id, String tenantId, AgentStatus status) {
        String sql = "UPDATE agents SET status = ?, updated_at = ? WHERE id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status.name());
            ps.setTimestamp(2, Timestamp.from(Instant.now()));
            ps.setString(3, id);
            ps.setString(4, tenantId);
            ps.executeUpdate();
            return findById(id, tenantId).orElseThrow();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to update agent status", ex);
        }
    }

    @Override
    public void delete(String id, String tenantId) {
        String sql = "DELETE FROM agents WHERE id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setString(2, tenantId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to delete agent", ex);
        }
    }

    private Agent map(ResultSet rs) throws SQLException {
        return new Agent(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("name"),
                rs.getString("description"),
                rs.getString("agent_type"),
                rs.getString("capabilities"),
                AgentStatus.valueOf(rs.getString("status")),
                rs.getTimestamp("created_at").toInstant(),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
