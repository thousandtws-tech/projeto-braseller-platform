package com.example.infrastructure.persistence;

import com.example.application.port.out.AgentContextRepository;
import com.example.domain.model.AgentContext;
import com.example.domain.model.ContextType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcAgentContextRepository implements AgentContextRepository {

    @Inject
    DataSource dataSource;

    @Override
    public AgentContext upsert(AgentContext c) {
        String sql = """
                INSERT INTO agent_contexts (id, tenant_id, agent_id, context_type, context_key, context_value, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (agent_id, tenant_id, context_type, context_key)
                DO UPDATE SET context_value = EXCLUDED.context_value, updated_at = EXCLUDED.updated_at
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, c.id());
            ps.setString(2, c.tenantId());
            ps.setString(3, c.agentId());
            ps.setString(4, c.contextType().name());
            ps.setString(5, c.contextKey());
            ps.setString(6, c.contextValue());
            ps.setTimestamp(7, Timestamp.from(c.updatedAt()));
            ps.executeUpdate();
            return c;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to upsert context", e);
        }
    }

    @Override
    public Optional<AgentContext> findByKey(String agentId, String tenantId, ContextType type, String key) {
        String sql = "SELECT * FROM agent_contexts WHERE agent_id = ? AND tenant_id = ? AND context_type = ? AND context_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            ps.setString(3, type.name());
            ps.setString(4, key);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return Optional.of(map(rs));
                return Optional.empty();
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to find context", e);
        }
    }

    @Override
    public List<AgentContext> findByAgent(String agentId, String tenantId) {
        String sql = "SELECT * FROM agent_contexts WHERE agent_id = ? AND tenant_id = ? ORDER BY updated_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AgentContext> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to list contexts", e);
        }
    }

    @Override
    public void deleteByKey(String agentId, String tenantId, ContextType type, String key) {
        String sql = "DELETE FROM agent_contexts WHERE agent_id = ? AND tenant_id = ? AND context_type = ? AND context_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            ps.setString(3, type.name());
            ps.setString(4, key);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new RepositoryException("Failed to delete context", e);
        }
    }

    private AgentContext map(ResultSet rs) throws SQLException {
        return new AgentContext(
                rs.getString("id"), rs.getString("tenant_id"), rs.getString("agent_id"),
                ContextType.valueOf(rs.getString("context_type")),
                rs.getString("context_key"), rs.getString("context_value"),
                rs.getTimestamp("updated_at").toInstant()
        );
    }
}
