package com.example.infrastructure.persistence;

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

import javax.sql.DataSource;

import com.example.application.port.out.UserIdentityRepository;
import com.example.domain.model.AccountantAccessView;
import com.example.domain.model.AccountantClientView;
import com.example.domain.model.EmailVerificationChallengeRecord;
import com.example.domain.model.RegisteredTenant;
import com.example.domain.model.StoredUserCredentials;
import com.example.domain.model.TenantCompanyProfile;
import com.example.domain.model.TenantView;
import com.example.domain.model.UserRole;
import com.example.domain.model.UserView;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class JdbcUserIdentityRepository implements UserIdentityRepository {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING_EMAIL_VERIFICATION = "PENDING_EMAIL_VERIFICATION";

    @Inject
    DataSource dataSource;

    @Override
    public RegisteredTenant registerTenant(String legalName, String tradeName, String adminName, String email,
                                           String passwordHash, TenantCompanyProfile companyProfile) {
        String tenantId = UUID.randomUUID().toString();
        String userId = UUID.randomUUID().toString();
        String normalizedEmail = normalizeEmail(email);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                insertTenant(connection, tenantId, legalName, tradeName, companyProfile);
                insertUser(connection, userId, tenantId, email, normalizedEmail, adminName, null, null, passwordHash,
                        "PASSWORD", null, STATUS_PENDING_EMAIL_VERIFICATION);
                insertRole(connection, tenantId, userId, UserRole.ADMIN);
                insertRole(connection, tenantId, userId, UserRole.VENDEDOR);
                connection.commit();
                return new RegisteredTenant(
                        toTenantView(tenantId, legalName, tradeName, companyProfile),
                        new UserView(
                                userId,
                                tenantId,
                                email,
                                adminName,
                                email,
                                null,
                                null,
                                null,
                                false,
                                "PASSWORD",
                                null,
                                STATUS_PENDING_EMAIL_VERIFICATION,
                                List.of(UserRole.ADMIN.name(), UserRole.VENDEDOR.name()),
                                List.of()
                        )
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
    public AccountantAccessView grantAccountantAccess(String accountantUserId, String tenantId, String accountantEmail,
                                                      String accountantFullName, String firstName, String lastName,
                                                      String passwordHash, String provider, String providerSubject,
                                                      String status, String grantedByUserId) {
        String accessId = UUID.randomUUID().toString();
        String normalizedEmail = normalizeEmail(accountantEmail);

        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                requireTenant(connection, tenantId);
                requireUser(connection, tenantId, grantedByUserId);
                if (!userExists(connection, accountantUserId)) {
                    insertUser(connection, accountantUserId, tenantId, accountantEmail, normalizedEmail,
                            accountantFullName, firstName, lastName, passwordHash, provider, providerSubject, status);
                }
                ensureRole(connection, tenantId, accountantUserId, UserRole.CONTADOR);

                Optional<AccountantAccessView> existingAccess = findActiveAccountantAccess(
                        connection, tenantId, accountantUserId, accountantEmail);
                if (existingAccess.isPresent()) {
                    connection.commit();
                    return existingAccess.get();
                }

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
                    statement.setString(6, STATUS_ACTIVE);
                    statement.executeUpdate();
                }

                connection.commit();
                return new AccountantAccessView(accessId, tenantId, accountantUserId, accountantEmail, true, STATUS_ACTIVE);
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
                     SELECT id, tenant_id, email, full_name, preferred_username, first_name, last_name, picture_url,
                            email_verified, provider, provider_subject, status
                     FROM user_accounts
                     WHERE tenant_id = ?
                     ORDER BY created_at ASC
                     """)) {
            statement.setString(1, tenantId);
            List<UserView> users = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    String userId = resultSet.getString("id");
                    users.add(toUserView(connection, resultSet, tenantId, userId));
                }
            }
            return users;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list tenant users", exception);
        }
    }

    @Override
    public List<AccountantClientView> listAccountantClients(String userId, String email) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT DISTINCT t.id AS tenant_id,
                            t.legal_name,
                            t.trade_name,
                            t.status AS tenant_status,
                            aa.read_only,
                            aa.status AS access_status,
                            aa.created_at
                     FROM accountant_access aa
                     JOIN tenants t ON t.id = aa.tenant_id
                     JOIN user_accounts ua ON ua.id = aa.accountant_user_id
                     WHERE aa.status = 'ACTIVE'
                       AND (aa.accountant_user_id = ? OR ua.email_normalized = ?)
                     ORDER BY t.trade_name NULLS LAST, t.legal_name
                     """)) {
            statement.setString(1, userId);
            statement.setString(2, normalizeEmail(email));
            List<AccountantClientView> clients = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    clients.add(new AccountantClientView(
                            resultSet.getString("tenant_id"),
                            resultSet.getString("legal_name"),
                            resultSet.getString("trade_name"),
                            resultSet.getString("tenant_status"),
                            resultSet.getBoolean("read_only"),
                            resultSet.getString("access_status"),
                            resultSet.getTimestamp("created_at").toInstant()
                    ));
                }
            }
            return clients;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list accountant clients", exception);
        }
    }

    @Override
    public List<AccountantClientView> listAllBpoClients() {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id AS tenant_id,
                            legal_name,
                            trade_name,
                            status AS tenant_status,
                            created_at
                     FROM tenants
                     ORDER BY trade_name NULLS LAST, legal_name
                     """)) {
            List<AccountantClientView> clients = new ArrayList<>();
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    clients.add(new AccountantClientView(
                            resultSet.getString("tenant_id"),
                            resultSet.getString("legal_name"),
                            resultSet.getString("trade_name"),
                            resultSet.getString("tenant_status"),
                            true,
                            "GLOBAL",
                            resultSet.getTimestamp("created_at").toInstant()
                    ));
                }
            }
            return clients;
        } catch (SQLException exception) {
            throw new RepositoryException("Could not list BPO clients", exception);
        }
    }

    @Override
    public List<String> listAccountantTenantIds(String userId, String email) {
        return listAccountantClients(userId, email).stream()
                .map(AccountantClientView::tenantId)
                .distinct()
                .toList();
    }

    @Override
    public Optional<UserView> findUserByEmail(String email) {
        try (Connection connection = dataSource.getConnection()) {
            return findUserByEmail(connection, normalizeEmail(email));
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find user by email", exception);
        }
    }

    @Override
    public Optional<StoredUserCredentials> findCredentialsByEmail(String email) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT id, tenant_id, email, full_name, preferred_username, first_name, last_name, picture_url,
                            email_verified, provider, provider_subject, password_hash, status
                     FROM user_accounts
                     WHERE email_normalized = ?
                     """)) {
            statement.setString(1, normalizeEmail(email));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String tenantId = resultSet.getString("tenant_id");
                String userId = resultSet.getString("id");
                return Optional.of(new StoredUserCredentials(
                        userId,
                        tenantId,
                        resultSet.getString("email"),
                        resultSet.getString("full_name"),
                        resultSet.getString("preferred_username"),
                        resultSet.getString("first_name"),
                        resultSet.getString("last_name"),
                        resultSet.getString("picture_url"),
                        resultSet.getBoolean("email_verified"),
                        resultSet.getString("provider"),
                        resultSet.getString("provider_subject"),
                        resultSet.getString("status"),
                        resultSet.getString("password_hash"),
                        listRoles(connection, tenantId, userId)
                ));
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not verify identity", exception);
        }
    }

    @Override
    public void saveEmailVerificationChallenge(String email, String codeHash, String codeSalt, Instant expiresAt,
                                               int attemptsRemaining, Instant lastSentAt) {
        String normalizedEmail = normalizeEmail(email);
        try (Connection connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try {
                String userId = requireUserIdByEmail(connection, normalizedEmail);
                try (PreparedStatement delete = connection.prepareStatement("""
                        DELETE FROM email_verification_codes
                        WHERE email_normalized = ?
                        """)) {
                    delete.setString(1, normalizedEmail);
                    delete.executeUpdate();
                }
                try (PreparedStatement statement = connection.prepareStatement("""
                        INSERT INTO email_verification_codes
                        (user_id, email_normalized, code_hash, code_salt, expires_at, attempts_remaining, last_sent_at, updated_at)
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                        """)) {
                    statement.setString(1, userId);
                    statement.setString(2, normalizedEmail);
                    statement.setString(3, codeHash);
                    statement.setString(4, codeSalt);
                    statement.setTimestamp(5, Timestamp.from(expiresAt));
                    statement.setInt(6, attemptsRemaining);
                    statement.setTimestamp(7, Timestamp.from(lastSentAt));
                    statement.setTimestamp(8, Timestamp.from(lastSentAt));
                    statement.executeUpdate();
                }
                connection.commit();
            } catch (SQLException exception) {
                connection.rollback();
                throw exception;
            } finally {
                connection.setAutoCommit(true);
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not save email verification challenge", exception);
        }
    }

    @Override
    public Optional<EmailVerificationChallengeRecord> findEmailVerificationChallenge(String email) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     SELECT email_normalized, code_hash, code_salt, expires_at, attempts_remaining, last_sent_at
                     FROM email_verification_codes
                     WHERE email_normalized = ?
                     """)) {
            statement.setString(1, normalizeEmail(email));
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new EmailVerificationChallengeRecord(
                        resultSet.getString("email_normalized"),
                        resultSet.getString("code_hash"),
                        resultSet.getString("code_salt"),
                        resultSet.getTimestamp("expires_at").toInstant(),
                        resultSet.getInt("attempts_remaining"),
                        resultSet.getTimestamp("last_sent_at").toInstant()
                ));
            }
        } catch (SQLException exception) {
            throw new RepositoryException("Could not find email verification challenge", exception);
        }
    }

    @Override
    public void updateEmailVerificationAttempts(String email, int attemptsRemaining, Instant updatedAt) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     UPDATE email_verification_codes
                     SET attempts_remaining = ?,
                         updated_at = ?
                     WHERE email_normalized = ?
                     """)) {
            statement.setInt(1, attemptsRemaining);
            statement.setTimestamp(2, Timestamp.from(updatedAt));
            statement.setString(3, normalizeEmail(email));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not update email verification attempts", exception);
        }
    }

    @Override
    public void deleteEmailVerificationChallenge(String email) {
        try (Connection connection = dataSource.getConnection();
             PreparedStatement statement = connection.prepareStatement("""
                     DELETE FROM email_verification_codes
                     WHERE email_normalized = ?
                     """)) {
            statement.setString(1, normalizeEmail(email));
            statement.executeUpdate();
        } catch (SQLException exception) {
            throw new RepositoryException("Could not delete email verification challenge", exception);
        }
    }

    @Override
    public Optional<UserView> activateUserEmail(String email, Instant updatedAt) {
        String normalizedEmail = normalizeEmail(email);
        try (Connection connection = dataSource.getConnection()) {
            int updated;
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE user_accounts
                    SET email_verified = TRUE,
                        status = ?,
                        updated_at = ?
                    WHERE email_normalized = ?
                    """)) {
                statement.setString(1, STATUS_ACTIVE);
                statement.setTimestamp(2, Timestamp.from(updatedAt));
                statement.setString(3, normalizedEmail);
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                return Optional.empty();
            }
            return findActiveUserByEmail(connection, normalizedEmail);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not activate user email", exception);
        }
    }

    @Override
    public Optional<UserView> syncExternalProfile(String email, String provider, String providerSubject, String fullName,
                                                  String preferredUsername, String firstName, String lastName,
                                                  String pictureUrl, boolean emailVerified) {
        String normalizedEmail = normalizeEmail(email);
        try (Connection connection = dataSource.getConnection()) {
            int updated;
            try (PreparedStatement statement = connection.prepareStatement("""
                    UPDATE user_accounts
                    SET full_name = COALESCE(NULLIF(?, ''), full_name),
                        provider = ?,
                        provider_subject = ?,
                        preferred_username = COALESCE(NULLIF(?, ''), preferred_username),
                        first_name = COALESCE(NULLIF(?, ''), first_name),
                        last_name = COALESCE(NULLIF(?, ''), last_name),
                        picture_url = COALESCE(NULLIF(?, ''), picture_url),
                        email_verified = ?,
                        updated_at = ?
                    WHERE email_normalized = ?
                    """)) {
                statement.setString(1, trimToEmpty(fullName));
                statement.setString(2, provider);
                statement.setString(3, providerSubject);
                statement.setString(4, trimToEmpty(preferredUsername));
                statement.setString(5, trimToEmpty(firstName));
                statement.setString(6, trimToEmpty(lastName));
                statement.setString(7, trimToEmpty(pictureUrl));
                statement.setBoolean(8, emailVerified);
                statement.setTimestamp(9, Timestamp.from(Instant.now()));
                statement.setString(10, normalizedEmail);
                updated = statement.executeUpdate();
            }
            if (updated == 0) {
                return Optional.empty();
            }
            return findUserByEmail(connection, normalizedEmail);
        } catch (SQLException exception) {
            throw new RepositoryException("Could not synchronize external profile", exception);
        }
    }

    private void insertTenant(Connection connection, String tenantId, String legalName, String tradeName,
                              TenantCompanyProfile companyProfile) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO tenants (
                    id, legal_name, trade_name, status,
                    cnpj, cnae_code, cnae_description,
                    address_street, address_number, address_complement, address_neighborhood,
                    address_city, address_state, address_zip_code
                )
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, legalName);
            statement.setString(3, tradeName);
            statement.setString(4, STATUS_ACTIVE);
            statement.setString(5, companyProfile.cnpj());
            statement.setString(6, companyProfile.cnaeCode());
            statement.setString(7, companyProfile.cnaeDescription());
            statement.setString(8, companyProfile.addressStreet());
            statement.setString(9, companyProfile.addressNumber());
            statement.setString(10, companyProfile.addressComplement());
            statement.setString(11, companyProfile.addressNeighborhood());
            statement.setString(12, companyProfile.addressCity());
            statement.setString(13, companyProfile.addressState());
            statement.setString(14, companyProfile.addressZipCode());
            statement.executeUpdate();
        }
    }

    private TenantView toTenantView(String tenantId, String legalName, String tradeName,
                                    TenantCompanyProfile companyProfile) {
        return new TenantView(
                tenantId,
                legalName,
                tradeName,
                STATUS_ACTIVE,
                companyProfile.cnpj(),
                companyProfile.cnaeCode(),
                companyProfile.cnaeDescription(),
                companyProfile.addressStreet(),
                companyProfile.addressNumber(),
                companyProfile.addressComplement(),
                companyProfile.addressNeighborhood(),
                companyProfile.addressCity(),
                companyProfile.addressState(),
                companyProfile.addressZipCode()
        );
    }

    private void insertUser(Connection connection, String userId, String tenantId, String email, String normalizedEmail,
                            String fullName, String firstName, String lastName, String passwordHash, String provider,
                            String providerSubject, String status) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                INSERT INTO user_accounts
                (id, tenant_id, email, email_normalized, full_name, first_name, last_name,
                 password_hash, provider, provider_subject, preferred_username, email_verified, status, updated_at)
                VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """)) {
            statement.setString(1, userId);
            statement.setString(2, tenantId);
            statement.setString(3, email);
            statement.setString(4, normalizedEmail);
            statement.setString(5, fullName);
            statement.setString(6, firstName);
            statement.setString(7, lastName);
            statement.setString(8, passwordHash);
            statement.setString(9, provider);
            statement.setString(10, providerSubject);
            statement.setString(11, email);
            statement.setBoolean(12, STATUS_ACTIVE.equals(status));
            statement.setString(13, status);
            statement.setTimestamp(14, Timestamp.from(Instant.now()));
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

    private void ensureRole(Connection connection, String tenantId, String userId, UserRole role) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT 1 FROM user_roles WHERE tenant_id = ? AND user_id = ? AND role = ?
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, userId);
            statement.setString(3, role.name());
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return;
                }
            }
        }
        insertRole(connection, tenantId, userId, role);
    }

    private boolean userExists(Connection connection, String userId) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("SELECT 1 FROM user_accounts WHERE id = ?")) {
            statement.setString(1, userId);
            try (ResultSet resultSet = statement.executeQuery()) {
                return resultSet.next();
            }
        }
    }

    private Optional<AccountantAccessView> findActiveAccountantAccess(Connection connection, String tenantId,
                                                                      String accountantUserId, String accountantEmail)
            throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, tenant_id, accountant_user_id, read_only, status
                FROM accountant_access
                WHERE tenant_id = ?
                  AND accountant_user_id = ?
                  AND status = 'ACTIVE'
                """)) {
            statement.setString(1, tenantId);
            statement.setString(2, accountantUserId);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                return Optional.of(new AccountantAccessView(
                        resultSet.getString("id"),
                        resultSet.getString("tenant_id"),
                        resultSet.getString("accountant_user_id"),
                        accountantEmail,
                        resultSet.getBoolean("read_only"),
                        resultSet.getString("status")
                ));
            }
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

    private String requireUserIdByEmail(Connection connection, String normalizedEmail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id FROM user_accounts WHERE email_normalized = ?
                """)) {
            statement.setString(1, normalizedEmail);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    throw new RepositoryException("User not found");
                }
                return resultSet.getString("id");
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

    private Optional<UserView> findActiveUserByEmail(Connection connection, String normalizedEmail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, tenant_id, email, full_name, preferred_username, first_name, last_name, picture_url,
                       email_verified, provider, provider_subject, status
                FROM user_accounts
                WHERE email_normalized = ?
                  AND status = 'ACTIVE'
                """)) {
            statement.setString(1, normalizedEmail);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String tenantId = resultSet.getString("tenant_id");
                String userId = resultSet.getString("id");
                return Optional.of(toUserView(connection, resultSet, tenantId, userId));
            }
        }
    }

    private Optional<UserView> findUserByEmail(Connection connection, String normalizedEmail) throws SQLException {
        try (PreparedStatement statement = connection.prepareStatement("""
                SELECT id, tenant_id, email, full_name, preferred_username, first_name, last_name, picture_url,
                       email_verified, provider, provider_subject, status
                FROM user_accounts
                WHERE email_normalized = ?
                """)) {
            statement.setString(1, normalizedEmail);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (!resultSet.next()) {
                    return Optional.empty();
                }
                String tenantId = resultSet.getString("tenant_id");
                String userId = resultSet.getString("id");
                return Optional.of(toUserView(connection, resultSet, tenantId, userId));
            }
        }
    }

    private UserView toUserView(Connection connection, ResultSet resultSet, String tenantId, String userId)
            throws SQLException {
        return new UserView(
                userId,
                tenantId,
                resultSet.getString("email"),
                resultSet.getString("full_name"),
                resultSet.getString("preferred_username"),
                resultSet.getString("first_name"),
                resultSet.getString("last_name"),
                resultSet.getString("picture_url"),
                resultSet.getBoolean("email_verified"),
                resultSet.getString("provider"),
                resultSet.getString("provider_subject"),
                resultSet.getString("status"),
                listRoles(connection, tenantId, userId),
                listAccountantTenantIds(userId, resultSet.getString("email"))
        );
    }

    private String trimToEmpty(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
