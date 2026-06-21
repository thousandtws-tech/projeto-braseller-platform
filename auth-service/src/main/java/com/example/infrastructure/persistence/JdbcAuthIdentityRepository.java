package com.example.infrastructure.persistence;

import com.example.application.port.out.AuthIdentityRepository;
import com.example.domain.model.AuthIdentity;
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
                        identity.fullName(), identity.roles(), identity.status(), identity.provider(),
                        identity.providerSubject(), identity.preferredUsername(), identity.firstName(),
                        identity.lastName(), identity.pictureUrl(), identity.emailVerified(),
                        identity.accountantTenantIds());
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
    public Optional<AuthIdentity> findIdentityByEmail(String email) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, tenant_id, user_id, email, full_name, roles, status, provider, provider_subject,
                            preferred_username, first_name, last_name, picture_url, email_verified,
                            accountant_tenant_ids
                     FROM auth_identities
                     WHERE email_normalized = ?
                       AND status = 'ACTIVE'
                       AND email_verified = TRUE
                     """)) {
            statement.setString(1, normalizeEmail(email));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toAuthIdentity(resultSet));
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find auth identity by email", exception);
        }
    }

    @Override
    public Optional<AuthIdentity> findAnyIdentityByEmail(String email) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, tenant_id, user_id, email, full_name, roles, status, provider, provider_subject,
                            preferred_username, first_name, last_name, picture_url, email_verified,
                            accountant_tenant_ids
                     FROM auth_identities
                     WHERE email_normalized = ?
                     """)) {
            statement.setString(1, normalizeEmail(email));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(toAuthIdentity(resultSet));
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find auth identity by email", exception);
        }
    }

    @Override
    public void linkProviderSubject(String email, String provider, String subject) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE auth_identities
                     SET provider = ?,
                         provider_subject = ?,
                         updated_at = ?
                     WHERE email_normalized = ?
                     """)) {
            statement.setString(1, provider);
            statement.setString(2, subject);
            statement.setTimestamp(3, Timestamp.from(Instant.now()));
            statement.setString(4, normalizeEmail(email));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not link external identity", exception);
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
                    provider_subject = ?,
                    preferred_username = ?,
                    first_name = ?,
                    last_name = ?,
                    picture_url = ?,
                    email_verified = ?,
                    accountant_tenant_ids = ?,
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
            statement.setString(8, blankToDefault(identity.provider(), "USER_SERVICE"));
            statement.setString(9, identity.providerSubject());
            statement.setString(10, identity.preferredUsername());
            statement.setString(11, identity.firstName());
            statement.setString(12, identity.lastName());
            statement.setString(13, identity.pictureUrl());
            statement.setBoolean(14, identity.emailVerified());
            statement.setString(15, String.join(",", identity.accountantTenantIds()));
            statement.setTimestamp(16, Timestamp.from(Instant.now()));
            statement.setString(17, normalizedEmail);
            return statement.executeUpdate();
        }
    }

    private void insertExternalIdentity(Connection connection, String identityId, AuthIdentity identity, String normalizedEmail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO auth_identities
                (id, tenant_id, user_id, email, email_normalized, full_name, password_hash, roles, status,
                 provider, provider_subject, preferred_username, first_name, last_name, picture_url,
                 email_verified, accountant_tenant_ids, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            statement.setString(10, blankToDefault(identity.provider(), "USER_SERVICE"));
            statement.setString(11, identity.providerSubject());
            statement.setString(12, identity.preferredUsername());
            statement.setString(13, identity.firstName());
            statement.setString(14, identity.lastName());
            statement.setString(15, identity.pictureUrl());
            statement.setBoolean(16, identity.emailVerified());
            statement.setString(17, String.join(",", identity.accountantTenantIds()));
            statement.setTimestamp(18, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    private AuthIdentity toAuthIdentity(ResultSet resultSet) throws SQLException {
        return new AuthIdentity(
                resultSet.getString("id"),
                resultSet.getString("tenant_id"),
                resultSet.getString("user_id"),
                resultSet.getString("email"),
                resultSet.getString("full_name"),
                splitList(resultSet.getString("roles")),
                resultSet.getString("status"),
                resultSet.getString("provider"),
                resultSet.getString("provider_subject"),
                resultSet.getString("preferred_username"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("picture_url"),
                resultSet.getBoolean("email_verified"),
                splitList(resultSet.getString("accountant_tenant_ids"))
        );
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private List<String> splitList(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .sorted()
                .toList();
    }

    private String blankToDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }
}
