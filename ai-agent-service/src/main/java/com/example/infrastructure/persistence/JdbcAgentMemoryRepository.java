package com.example.infrastructure.persistence;

import com.example.application.port.out.AgentMemoryRepository;
import com.example.domain.model.AgentMemory;
import com.example.domain.model.MemoryType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class JdbcAgentMemoryRepository implements AgentMemoryRepository {

    @Inject
    DataSource dataSource;

    @Override
    public AgentMemory save(AgentMemory memory) {
        String sql = """
                INSERT INTO agent_memories (id, tenant_id, agent_id, memory_type, memory_key, memory_value,
                    ttl_seconds, created_at, expires_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                ON CONFLICT (agent_id, tenant_id, memory_key) DO UPDATE
                    SET memory_value = EXCLUDED.memory_value, ttl_seconds = EXCLUDED.ttl_seconds,
                        expires_at = EXCLUDED.expires_at
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, memory.id());
            ps.setString(2, memory.tenantId());
            ps.setString(3, memory.agentId());
            ps.setString(4, memory.memoryType().name());
            ps.setString(5, memory.memoryKey());
            ps.setString(6, memory.memoryValue());
            ps.setObject(7, memory.ttlSeconds());
            ps.setTimestamp(8, Timestamp.from(memory.createdAt()));
            ps.setTimestamp(9, memory.expiresAt() != null ? Timestamp.from(memory.expiresAt()) : null);
            ps.executeUpdate();
            return memory;
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to save memory", ex);
        }
    }

    @Override
    public Optional<AgentMemory> findByKey(String agentId, String tenantId, String memoryKey) {
        String sql = "SELECT * FROM agent_memories WHERE agent_id = ? AND tenant_id = ? AND memory_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            ps.setString(3, memoryKey);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(map(rs)) : Optional.empty();
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to find memory", ex);
        }
    }

    @Override
    public List<AgentMemory> findByAgent(String agentId, String tenantId, MemoryType memoryType) {
        String sql = memoryType != null
                ? "SELECT * FROM agent_memories WHERE agent_id = ? AND tenant_id = ? AND memory_type = ? AND (expires_at IS NULL OR expires_at > NOW())"
                : "SELECT * FROM agent_memories WHERE agent_id = ? AND tenant_id = ? AND (expires_at IS NULL OR expires_at > NOW())";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            if (memoryType != null) ps.setString(3, memoryType.name());
            try (ResultSet rs = ps.executeQuery()) {
                List<AgentMemory> list = new ArrayList<>();
                while (rs.next()) list.add(map(rs));
                return list;
            }
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to list memories", ex);
        }
    }

    @Override
    public void deleteExpired() {
        String sql = "DELETE FROM agent_memories WHERE expires_at IS NOT NULL AND expires_at < NOW()";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to delete expired memories", ex);
        }
    }

    @Override
    public void deleteByKey(String agentId, String tenantId, String memoryKey) {
        String sql = "DELETE FROM agent_memories WHERE agent_id = ? AND tenant_id = ? AND memory_key = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            ps.setString(3, memoryKey);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to delete memory", ex);
        }
    }

    @Override
    public void deleteShortTermByAgent(String agentId, String tenantId) {
        String sql = "DELETE FROM agent_memories WHERE agent_id = ? AND tenant_id = ? AND memory_type = 'SHORT_TERM'";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            ps.executeUpdate();
        } catch (SQLException ex) {
            throw new RepositoryException("Failed to delete short-term memories", ex);
        }
    }

    private AgentMemory map(ResultSet rs) throws SQLException {
        Timestamp expiresAt = rs.getTimestamp("expires_at");
        Integer ttl = (Integer) rs.getObject("ttl_seconds");
        return new AgentMemory(
                rs.getString("id"),
                rs.getString("tenant_id"),
                rs.getString("agent_id"),
                MemoryType.valueOf(rs.getString("memory_type")),
                rs.getString("memory_key"),
                rs.getString("memory_value"),
                ttl,
                rs.getTimestamp("created_at").toInstant(),
                expiresAt != null ? expiresAt.toInstant() : null
        );
    }
}
