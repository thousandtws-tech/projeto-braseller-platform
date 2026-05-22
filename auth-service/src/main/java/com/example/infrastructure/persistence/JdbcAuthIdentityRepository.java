package com.example.infrastructure.persistence;

import com.example.application.port.out.AuthIdentityRepository;
import com.example.domain.model.AuthIdentity;
import com.example.domain.model.AuthSession;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcAuthIdentityRepository implements AuthIdentityRepository {
    @Inject
    DataSource dataSource;

    @Override
    public AuthIdentity synchronize(AuthIdentity identity) {
        String identityId = identity.id() == null ? UUID.randomUUID().toString() : identity.id();
        String normalizedEmail = normalizeEmail(identity.email());
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                int updated = updateIdentity(connection, identity, normalizedEmail);
                if (updated == 0) {
                    insertExternalIdentity(connection, identityId, identity, normalizedEmail);
                }
                connection.commit();
                return new AuthIdentity(identityId, identity.tenantId(), identity.userId(), identity.email(),
                        identity.fullName(), identity.roles(), identity.status());
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not synchronize auth identity", exception);
        }
    }

    @Override
    public void createSession(AuthSession session) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     INSERT INTO auth_sessions
                     (id, tenant_id, user_id, token_id, refresh_token_hash, expires_at)
                     VALUES (?, ?, ?, ?, ?, ?)
                     """)) {
            statement.setString(1, session.id());
            statement.setString(2, session.tenantId());
            statement.setString(3, session.userId());
            statement.setString(4, session.tokenId());
            statement.setString(5, session.refreshTokenHash());
            statement.setTimestamp(6, Timestamp.from(session.expiresAt()));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not create auth session", exception);
        }
    }

    @Override
    public Optional<AuthIdentity> findIdentityByRefreshTokenHash(String refreshTokenHash) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT i.id, i.tenant_id, i.user_id, i.email, i.full_name, i.roles, i.status
                     FROM auth_sessions s
                     JOIN auth_identities i ON i.user_id = s.user_id AND i.tenant_id = s.tenant_id
                     WHERE s.refresh_token_hash = ?
                       AND s.revoked_at IS NULL
                       AND s.expires_at > ?
                       AND i.status = 'ACTIVE'
                     """)) {
            statement.setString(1, refreshTokenHash);
            statement.setTimestamp(2, Timestamp.from(Instant.now()));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AuthIdentity(
                        resultSet.getString("id"),
                        resultSet.getString("tenant_id"),
                        resultSet.getString("user_id"),
                        resultSet.getString("email"),
                        resultSet.getString("full_name"),
                        splitRoles(resultSet.getString("roles")),
                        resultSet.getString("status")
                ));
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not refresh auth session", exception);
        }
    }

    @Override
    public boolean revokeRefreshToken(String refreshTokenHash) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE auth_sessions
                     SET revoked_at = ?
                     WHERE refresh_token_hash = ? AND revoked_at IS NULL
                     """)) {
            statement.setTimestamp(1, Timestamp.from(Instant.now()));
            statement.setString(2, refreshTokenHash);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not revoke auth session", exception);
        }
    }

    private int updateIdentity(Connection connection, AuthIdentity identity, String normalizedEmail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                UPDATE auth_identities
                SET tenant_id = ?,
                    user_id = ?,
                    email = ?,
                    full_name = ?,
                    roles = ?,
                    status = ?,
                    password_hash = ?,
                    provider = ?,
                    updated_at = ?
                WHERE email_normalized = ?
                """)) {
            statement.setString(1, identity.tenantId());
            statement.setString(2, identity.userId());
            statement.setString(3, identity.email());
            statement.setString(4, identity.fullName());
            statement.setString(5, String.join(",", identity.roles()));
            statement.setString(6, identity.status());
            statement.setString(7, "external:user-service");
            statement.setString(8, "USER_SERVICE");
            statement.setTimestamp(9, Timestamp.from(Instant.now()));
            statement.setString(10, normalizedEmail);
            return statement.executeUpdate();
        }
    }

    private void insertExternalIdentity(Connection connection, String identityId, AuthIdentity identity, String normalizedEmail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO auth_identities
                (id, tenant_id, user_id, email, email_normalized, full_name, password_hash, roles, status, provider, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, identityId);
            statement.setString(2, identity.tenantId());
            statement.setString(3, identity.userId());
            statement.setString(4, identity.email());
            statement.setString(5, normalizedEmail);
            statement.setString(6, identity.fullName());
            statement.setString(7, "external:user-service");
            statement.setString(8, String.join(",", identity.roles()));
            statement.setString(9, identity.status());
            statement.setString(10, "USER_SERVICE");
            statement.setTimestamp(11, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> splitRoles(String roles) {
        return Arrays.stream(roles.split(","))
                .map(String::trim)
                .filter(role -> !role.isEmpty())
                .sorted()
                .toList();
    }
}
