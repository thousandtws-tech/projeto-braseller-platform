package com.example.application.service;

import com.example.application.command.GrantAccountantAccessCommand;
import com.example.application.command.RegisterTenantCommand;
import com.example.application.command.SyncExternalProfileCommand;
import com.example.application.command.VerifyPasswordCommand;
import com.example.application.exception.ConflictException;
import com.example.application.exception.ForbiddenException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.InternalServiceAuthorizer;
import com.example.application.port.out.PasswordHasher;
import com.example.application.port.out.UserIdentityRepository;
import com.example.domain.model.AccountantAccessView;
import com.example.domain.model.AccountantClientView;
import com.example.domain.model.IdentityVerification;
import com.example.domain.model.RegisteredTenant;
import com.example.domain.model.StoredUserCredentials;
import com.example.domain.model.TenantCompanyProfile;
import com.example.domain.model.TenantContext;
import com.example.domain.model.UserView;
import com.example.infrastructure.keycloak.KeycloakAdminClient;
import com.example.infrastructure.keycloak.KeycloakIntegrationException;
import com.example.infrastructure.persistence.RepositoryException;
import io.quarkus.cache.CacheInvalidate;
import io.quarkus.cache.CacheInvalidateAll;
import io.quarkus.cache.CacheResult;
import io.smallrye.mutiny.Uni;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.logging.Logger;

@ApplicationScoped
public class UserIdentityService {

    private static final Logger LOG = Logger.getLogger(UserIdentityService.class.getName());

    @Inject
    UserIdentityRepository userIdentityRepository;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    InternalServiceAuthorizer internalServiceAuthorizer;

    @Inject
    KeycloakAdminClient keycloakAdminClient;

    @CacheInvalidate(cacheName = "user-members")
    public RegisteredTenant registerTenant(RegisterTenantCommand command) {
        if (isBlank(command.legalName()) || isBlank(command.adminName()) || isBlank(command.email()) || isWeakPassword(command.password())) {
            throw new ValidationException("legalName, adminName, email and a password with at least 8 characters are required");
        }

        try {
            return userIdentityRepository.registerTenant(
                    command.legalName().trim(),
                    blankToNull(command.tradeName()),
                    command.adminName().trim(),
                    command.email().trim(),
                    passwordHasher.hash(command.password()),
                    new TenantCompanyProfile(
                            onlyDigitsOrNull(command.cnpj()),
                            blankToNull(command.cnaeCode()),
                            blankToNull(command.cnaeDescription()),
                            blankToNull(command.addressStreet()),
                            blankToNull(command.addressNumber()),
                            blankToNull(command.addressComplement()),
                            blankToNull(command.addressNeighborhood()),
                            blankToNull(command.addressCity()),
                            blankToNull(command.addressState()),
                            onlyDigitsOrNull(command.addressZipCode())
                    )
            );
        } catch (RepositoryException exception) {
            throw new ConflictException(exception.getMessage());
        }
    }

    @CacheInvalidate(cacheName = "user-members")
    public AccountantAccessView grantAccountantAccess(GrantAccountantAccessCommand command) {
        LOG.info("Granting accountant access to " + command.email());
        if (isBlank(command.email()) || isBlank(command.firstName()) || isBlank(command.lastName())
                || isBlank(command.grantedByUserId())) {
            throw new ValidationException("email, firstName, lastName and grantedByUserId are required");
        }

        String email      = command.email().trim();
        String firstName  = command.firstName().trim();
        String lastName   = command.lastName().trim();
        String fullName   = command.fullName() != null ? command.fullName().trim() : firstName + " " + lastName;
        String rawPassword = command.temporaryPassword() != null ? command.temporaryPassword() : DEFAULT_TEMPORARY_PASSWORD;
        Optional<UserView> existingUser = userIdentityRepository.findUserByEmail(email);
        String accountantUserId = existingUser.map(UserView::id).orElseGet(() -> UUID.randomUUID().toString());

        // 1. Tenta criar o usuário no Keycloak primeiro
        Optional<String> keycloakSubject = Optional.empty();
        if (existingUser.isEmpty()) {
            try {
                keycloakSubject = keycloakAdminClient.createAccountantUser(
                        accountantUserId, command.tenantId(), email, firstName, lastName, rawPassword);
            } catch (KeycloakIntegrationException e) {
                throw new ValidationException("Falha ao criar usuario no Keycloak: " + e.getMessage());
            }
        }

        // 2. Determina provider/status conforme resultado do Keycloak
        String provider        = existingUser.map(UserView::provider).orElse(keycloakSubject.isPresent() ? "KEYCLOAK" : "PASSWORD");
        String providerSubject = existingUser.map(UserView::providerSubject).orElse(keycloakSubject.orElse(null));
        String status          = existingUser.map(UserView::status).orElse(keycloakSubject.isPresent() ? "ACTIVE" : "INVITED");

        // Usuários Keycloak não usam senha local; armazenamos hash de valor inacessível
        String passwordHash = keycloakSubject.isPresent()
                ? "KEYCLOAK_MANAGED_" + UUID.randomUUID()
                : passwordHasher.hash(rawPassword);

        // 3. Persiste localmente
        try {
            return userIdentityRepository.grantAccountantAccess(
                    accountantUserId,
                    command.tenantId(),
                    email,
                    fullName,
                    firstName,
                    lastName,
                    passwordHash,
                    provider,
                    providerSubject,
                    status,
                    command.grantedByUserId()
            );
        } catch (RepositoryException exception) {
            throw new ValidationException(exception.getMessage());
        }
    }

    @CacheResult(cacheName = "user-members")
    public List<UserView> listTenantMembers(String tenantId) {
        LOG.fine("Listing members for tenant " + tenantId);
        return userIdentityRepository.listTenantUsers(tenantId);
    }

    public List<AccountantClientView> listAccountantClients(TenantContext context) {
        if (isGlobalBpoOperator(context)) {
            return userIdentityRepository.listAllBpoClients();
        }
        if (!context.roles().contains("CONTADOR")) {
            throw new ForbiddenException("accountant_role_required");
        }
        return userIdentityRepository.listAccountantClients(context.userId(), context.email());
    }

    public Optional<IdentityVerification> verifyPassword(String internalToken, VerifyPasswordCommand command) {
        if (!internalServiceAuthorizer.isAuthorized(internalToken)) {
            throw new ForbiddenException("invalid_internal_token");
        }
        if (isBlank(command.email()) || isBlank(command.password())) {
            throw new ValidationException("email and password are required");
        }

        return userIdentityRepository.findActiveCredentialsByEmail(command.email())
                .filter(credentials -> passwordHasher.verify(command.password(), credentials.passwordHash()))
                .map(this::toVerification);
    }

    public Optional<UserView> findIdentityByEmail(String internalToken, String email) {
        if (!internalServiceAuthorizer.isAuthorized(internalToken)) {
            throw new ForbiddenException("invalid_internal_token");
        }
        if (isBlank(email)) {
            throw new ValidationException("email is required");
        }

        return userIdentityRepository.findUserByEmail(email.trim());
    }

    @CacheInvalidateAll(cacheName = "user-members")
    public Optional<UserView> markEmailVerified(String internalToken, String email) {
        if (!internalServiceAuthorizer.isAuthorized(internalToken)) {
            throw new ForbiddenException("invalid_internal_token");
        }
        if (isBlank(email)) {
            throw new ValidationException("email is required");
        }

        return userIdentityRepository.markEmailVerifiedByEmail(email.trim());
    }

    public Optional<UserView> resetPassword(String internalToken, String email, String newPassword) {
        if (!internalServiceAuthorizer.isAuthorized(internalToken)) {
            throw new ForbiddenException("invalid_internal_token");
        }
        if (isBlank(email) || isWeakPassword(newPassword)) {
            throw new ValidationException("email and a password with at least 8 characters are required");
        }

        return userIdentityRepository.updatePasswordByEmail(email.trim(), passwordHasher.hash(newPassword));
    }

    @CacheInvalidateAll(cacheName = "user-members")
    public Optional<UserView> syncExternalProfile(String internalToken, SyncExternalProfileCommand command) {
        if (!internalServiceAuthorizer.isAuthorized(internalToken)) {
            throw new ForbiddenException("invalid_internal_token");
        }
        if (isBlank(command.email()) || isBlank(command.provider()) || isBlank(command.providerSubject())) {
            throw new ValidationException("email, provider and providerSubject are required");
        }

        return userIdentityRepository.syncExternalProfile(
                command.email().trim(),
                command.provider().trim().toUpperCase(),
                command.providerSubject().trim(),
                blankToNull(command.fullName()),
                blankToNull(command.preferredUsername()),
                blankToNull(command.firstName()),
                blankToNull(command.lastName()),
                blankToNull(command.pictureUrl()),
                command.emailVerified()
        );
    }

    private IdentityVerification toVerification(StoredUserCredentials credentials) {
        return new IdentityVerification(
                credentials.userId(),
                credentials.tenantId(),
                credentials.email(),
                credentials.fullName(),
                credentials.preferredUsername(),
                credentials.firstName(),
                credentials.lastName(),
                credentials.pictureUrl(),
                credentials.emailVerified(),
                credentials.provider(),
                credentials.providerSubject(),
                credentials.roles(),
                userIdentityRepository.listAccountantTenantIds(credentials.userId(), credentials.email())
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isWeakPassword(String value) {
        return value == null || value.length() < 8;
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value.trim();
    }

    private String onlyDigitsOrNull(String value) {
        if (isBlank(value)) {
            return null;
        }
        String digits = value.replaceAll("\\D", "");
        return digits.isBlank() ? null : digits;
    }

    private boolean isGlobalBpoOperator(TenantContext context) {
        return context.roles().contains("BPO_ADMIN")
                || (context.roles().contains("ADMIN") && context.roles().contains("CONTADOR"));
    }
}
