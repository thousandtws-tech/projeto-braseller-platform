package com.example.infrastructure.persistence;

import com.example.application.port.out.AgentFeedbackRepository;
import com.example.domain.model.AgentFeedback;
import com.example.domain.model.FeedbackType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class JdbcAgentFeedbackRepository implements AgentFeedbackRepository {

    @Inject
    DataSource dataSource;

    @Override
    public AgentFeedback save(AgentFeedback f) {
        String sql = """
                INSERT INTO agent_feedbacks
                (id, tenant_id, agent_id, execution_id, feedback_type, score, comment, metadata_json, created_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, f.id());
            ps.setString(2, f.tenantId());
            ps.setString(3, f.agentId());
            ps.setString(4, f.executionId());
            ps.setString(5, f.feedbackType().name());
            ps.setInt(6, f.score());
            ps.setString(7, f.comment());
            ps.setString(8, f.metadataJson());
            ps.setTimestamp(9, Timestamp.from(f.createdAt()));
            ps.executeUpdate();
            return f;
        } catch (SQLException e) {
            throw new RepositoryException("Failed to save feedback", e);
        }
    }

    @Override
    public List<AgentFeedback> findByAgentId(String agentId, String tenantId, int limit) {
        String sql = "SELECT * FROM agent_feedbacks WHERE agent_id = ? AND tenant_id = ? ORDER BY created_at DESC LIMIT ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            ps.setInt(3, limit);
            try (ResultSet rs = ps.executeQuery()) {
                List<AgentFeedback> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to list feedbacks", e);
        }
    }

    @Override
    public List<AgentFeedback> findByExecutionId(String executionId, String tenantId) {
        String sql = "SELECT * FROM agent_feedbacks WHERE execution_id = ? AND tenant_id = ? ORDER BY created_at DESC";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, executionId);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                List<AgentFeedback> result = new ArrayList<>();
                while (rs.next()) result.add(map(rs));
                return result;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to list feedbacks by execution", e);
        }
    }

    @Override
    public double averageScoreByAgent(String agentId, String tenantId) {
        String sql = "SELECT COALESCE(AVG(score), 0.0) FROM agent_feedbacks WHERE agent_id = ? AND tenant_id = ?";
        try (Connection conn = dataSource.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, agentId);
            ps.setString(2, tenantId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble(1);
                return 0.0;
            }
        } catch (SQLException e) {
            throw new RepositoryException("Failed to compute average score", e);
        }
    }

    private AgentFeedback map(ResultSet rs) throws SQLException {
        return new AgentFeedback(
                rs.getString("id"), rs.getString("tenant_id"), rs.getString("agent_id"),
                rs.getString("execution_id"),
                FeedbackType.valueOf(rs.getString("feedback_type")),
                rs.getInt("score"), rs.getString("comment"),
                rs.getString("metadata_json"),
                rs.getTimestamp("created_at").toInstant()
        );
    }
}
