package com.example.infrastructure.persistence;

import com.example.application.port.out.AuthChallengeRepository;
import com.example.domain.model.AuthChallenge;
import com.example.domain.model.AuthChallengeType;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcAuthChallengeRepository implements AuthChallengeRepository {
    @Inject
    DataSource dataSource;

    @Override
    public AuthChallenge create(String email, String normalizedEmail, AuthChallengeType type, String codeHash,
                                Instant expiresAt, String requestIp, boolean subjectExists) {
        String id = UUID.randomUUID().toString();
        Instant now = Instant.now();
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO auth_challenges
                     (id, challenge_type, email, email_normalized, code_hash, expires_at, subject_exists, request_ip, created_at)
                     VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, id);
            statement.setString(2, type.name());
            statement.setString(3, email);
            statement.setString(4, normalizedEmail);
            statement.setString(5, codeHash);
            statement.setTimestamp(6, Timestamp.from(expiresAt));
            statement.setBoolean(7, subjectExists);
            statement.setString(8, requestIp);
            statement.setTimestamp(9, Timestamp.from(now));
            statement.executeUpdate();
            return new AuthChallenge(id, type, email, normalizedEmail, codeHash, expiresAt, null, 0, subjectExists,
                    requestIp, now);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not create auth challenge", exception);
        }
    }

    @Override
    public void invalidateOpenChallenges(String normalizedEmail, AuthChallengeType type) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE auth_challenges
                     SET used_at = ?
                     WHERE email_normalized = ?
                       AND challenge_type = ?
                       AND used_at IS NULL
                     """)) {
            statement.setTimestamp(1, Timestamp.from(Instant.now()));
            statement.setString(2, normalizedEmail);
            statement.setString(3, type.name());
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not invalidate auth challenges", exception);
        }
    }

    @Override
    public Optional<AuthChallenge> findLatestOpenChallenge(String normalizedEmail, AuthChallengeType type, Instant now) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, challenge_type, email, email_normalized, code_hash, expires_at, used_at, attempts,
                            subject_exists, request_ip, created_at
                     FROM auth_challenges
                     WHERE email_normalized = ?
                       AND challenge_type = ?
                       AND used_at IS NULL
                       AND expires_at > ?
                     ORDER BY created_at DESC
                     LIMIT 1
                     """)) {
            statement.setString(1, normalizedEmail);
            statement.setString(2, type.name());
            statement.setTimestamp(3, Timestamp.from(now));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toChallenge(resultSet));
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find auth challenge", exception);
        }
    }

    @Override
    public void incrementAttempts(String challengeId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE auth_challenges
                     SET attempts = attempts + 1
                     WHERE id = ?
                     """)) {
            statement.setString(1, challengeId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not increment auth challenge attempts", exception);
        }
    }

    @Override
    public void consume(String challengeId, Instant usedAt) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE auth_challenges
                     SET used_at = ?
                     WHERE id = ?
                       AND used_at IS NULL
                     """)) {
            statement.setTimestamp(1, Timestamp.from(usedAt));
            statement.setString(2, challengeId);
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not consume auth challenge", exception);
        }
    }

    @Override
    public long countRecentRequests(String normalizedEmail, String requestIp, AuthChallengeType type, Instant since) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT COUNT(*) AS total
                     FROM auth_challenges
                     WHERE challenge_type = ?
                       AND created_at >= ?
                       AND (email_normalized = ? OR (? IS NOT NULL AND request_ip = ?))
                     """)) {
            statement.setString(1, type.name());
            statement.setTimestamp(2, Timestamp.from(since));
            statement.setString(3, normalizedEmail);
            statement.setString(4, requestIp);
            statement.setString(5, requestIp);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next() ? resultSet.getLong("total") : 0;
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not count auth challenge requests", exception);
        }
    }

    private AuthChallenge toChallenge(ResultSet resultSet) throws SQLException {
        Timestamp usedAt = resultSet.getTimestamp("used_at");
        return new AuthChallenge(
                resultSet.getString("id"),
                AuthChallengeType.valueOf(resultSet.getString("challenge_type")),
                resultSet.getString("email"),
                resultSet.getString("email_normalized"),
                resultSet.getString("code_hash"),
                resultSet.getTimestamp("expires_at").toInstant(),
                usedAt == null ? null : usedAt.toInstant(),
                resultSet.getInt("attempts"),
                resultSet.getBoolean("subject_exists"),
                resultSet.getString("request_ip"),
                resultSet.getTimestamp("created_at").toInstant()
        );
    }
}
