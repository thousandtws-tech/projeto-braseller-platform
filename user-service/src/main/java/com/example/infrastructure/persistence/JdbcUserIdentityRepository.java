package com.example.infrastructure.persistence;

import com.example.application.port.out.UserIdentityRepository;
import com.example.domain.model.AccountantAccessView;
import com.example.domain.model.RegisteredTenant;
import com.example.domain.model.StoredUserCredentials;
import com.example.domain.model.TenantView;
import com.example.domain.model.UserRole;
import com.example.domain.model.UserView;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

@ApplicationScoped
public class JdbcUserIdentityRepository implements UserIdentityRepository {
    @Inject
    DataSource dataSource;

    @Override
    public RegisteredTenant registerTenant(String legalName, String tradeName, String adminName, String email, String passwordHash) {
        String tenantId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String normalizedEmail = normalizeEmail(email);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertTenant(connection, tenantId, legalName, tradeName);
                insertUser(connection, userId, tenantId, email, normalizedEmail, adminName, passwordHash, "PASSWORD", null, "ACTIVE");
                insertRole(connection, tenantId, userId, UserRole.ADMIN);
                insertRole(connection, tenantId, userId, UserRole.VENDEDOR);
                connection.commit();
                return new RegisteredTenant(
                        new TenantView(tenantId, legalName, tradeName, "ACTIVE"),
                        new UserView(userId, tenantId, email, adminName, "ACTIVE", List.of(UserRole.ADMIN.name(), UserRole.VENDEDOR.name()))
                );
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not register tenant", exception);
        }
    }

    @Override
    public AccountantAccessView grantAccountantAccess(
            String tenantId,
            String accountantEmail,
            String accountantName,
            String temporaryPasswordHash,
            String grantedByUserId
    ) {
        String accountantUserId = UUID.randomUUID().toString();
        String accessId = UUID.randomUUID().toString();
        String normalizedEmail = normalizeEmail(accountantEmail);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                requireTenant(connection, tenantId);
                requireUser(connection, tenantId, grantedByUserId);
                insertUser(connection, accountantUserId, tenantId, accountantEmail, normalizedEmail, accountantName,
                        temporaryPasswordHash, "PASSWORD", null, "INVITED");
                insertRole(connection, tenantId, accountantUserId, UserRole.CONTADOR);

                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO accountant_access
                        (id, tenant_id, accountant_user_id, granted_by_user_id, read_only, status)
                        VALUES (?, ?, ?, ?, ?, ?)
                        """)) {
                    statement.setString(1, accessId);
                    statement.setString(2, tenantId);
                    statement.setString(3, accountantUserId);
                    statement.setString(4, grantedByUserId);
                    statement.setBoolean(5, true);
                    statement.setString(6, "ACTIVE");
                    statement.executeUpdate();
                }

                connection.commit();
                return new AccountantAccessView(accessId, tenantId, accountantUserId, accountantEmail, true, "ACTIVE");
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not grant accountant access", exception);
        }
    }

    @Override
    public List<UserView> listTenantUsers(String tenantId) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, tenant_id, email, full_name, status
                     FROM user_accounts
                     WHERE tenant_id = ?
                     ORDER BY created_at ASC
                     """)) {
            statement.setString(1, tenantId);
            List<UserView> users = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String userId = resultSet.getString("id");
                    users.add(new UserView(
                            userId,
                            resultSet.getString("tenant_id"),
                            resultSet.getString("email"),
                            resultSet.getString("full_name"),
                            resultSet.getString("status"),
                            listRoles(connection, tenantId, userId)
                    ));
                }
            }
            return users;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list tenant users", exception);
        }
    }

    @Override
    public Optional<StoredUserCredentials> findActiveCredentialsByEmail(String email) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, tenant_id, email, full_name, password_hash, status
                     FROM user_accounts
                     WHERE email_normalized = ?
                     """)) {
            statement.setString(1, normalizeEmail(email));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next() || !"ACTIVE".equals(resultSet.getString("status"))) {
                    return Optional.empty();
                }
                String tenantId = resultSet.getString("tenant_id");
                String userId = resultSet.getString("id");
                return Optional.of(new StoredUserCredentials(
                        userId,
                        tenantId,
                        resultSet.getString("email"),
                        resultSet.getString("full_name"),
                        resultSet.getString("password_hash"),
                        listRoles(connection, tenantId, userId)
                ));
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not verify identity", exception);
        }
    }

    private void insertTenant(Connection connection, String tenantId, String legalName, String tradeName) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tenants (id, legal_name, trade_name, status)
                VALUES (?, ?, ?, ?)
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, legalName);
            statement.setString(3, tradeName);
            statement.setString(4, "ACTIVE");
            statement.executeUpdate();
        }
    }

    private void insertUser(
            Connection connection,
            String userId,
            String tenantId,
            String email,
            String normalizedEmail,
            String fullName,
            String passwordHash,
            String provider,
            String providerSubject,
            String status
    ) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_accounts
                (id, tenant_id, email, email_normalized, full_name, password_hash, provider, provider_subject, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, userId);
            statement.setString(2, tenantId);
            statement.setString(3, email);
            statement.setString(4, normalizedEmail);
            statement.setString(5, fullName);
            statement.setString(6, passwordHash);
            statement.setString(7, provider);
            statement.setString(8, providerSubject);
            statement.setString(9, status);
            statement.setTimestamp(10, Timestamp.from(Instant.now()));
            statement.executeUpdate();
        }
    }

    private void insertRole(Connection connection, String tenantId, String userId, UserRole role) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_roles (tenant_id, user_id, role)
                VALUES (?, ?, ?)
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, userId);
            statement.setString(3, role.name());
            statement.executeUpdate();
        }
    }

    private void requireTenant(Connection connection, String tenantId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT id FROM tenants WHERE id = ?")) {
            statement.setString(1, tenantId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new RepositoryException("Tenant not found");
                }
            }
        }
    }

    private void requireUser(Connection connection, String tenantId, String userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id FROM user_accounts WHERE tenant_id = ? AND id = ?
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new RepositoryException("User not found for tenant");
                }
            }
        }
    }

    private List<String> listRoles(Connection connection, String tenantId, String userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT role FROM user_roles WHERE tenant_id = ? AND user_id = ? ORDER BY role
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, userId);
            List<String> roles = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    roles.add(resultSet.getString("role"));
                }
            }
            return roles;
        }
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
