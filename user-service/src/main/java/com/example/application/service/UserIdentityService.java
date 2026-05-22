package com.example.application.service;

import com.example.application.command.GrantAccountantAccessCommand;
import com.example.application.command.RegisterTenantCommand;
import com.example.application.command.VerifyPasswordCommand;
import com.example.application.exception.ConflictException;
import com.example.application.exception.ForbiddenException;
import com.example.application.exception.TenantMismatchException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.InternalServiceAuthorizer;
import com.example.application.port.out.PasswordHasher;
import com.example.application.port.out.UserIdentityRepository;
import com.example.domain.model.AccountantAccessView;
import com.example.domain.model.IdentityVerification;
import com.example.domain.model.RegisteredTenant;
import com.example.domain.model.StoredUserCredentials;
import com.example.domain.model.UserView;
import com.example.infrastructure.persistence.RepositoryException;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Optional;

@ApplicationScoped
public class UserIdentityService {
    private static final String DEFAULT_TEMPORARY_PASSWORD = "ChangeMe123!";

    @Inject
    UserIdentityRepository userIdentityRepository;

    @Inject
    PasswordHasher passwordHasher;

    @Inject
    InternalServiceAuthorizer internalServiceAuthorizer;

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
                    passwordHasher.hash(command.password())
            );
        } catch (RepositoryException exception) {
            throw new ConflictException(exception.getMessage());
        }
    }

    public AccountantAccessView grantAccountantAccess(GrantAccountantAccessCommand command) {
        if (isBlank(command.email()) || isBlank(command.fullName()) || isBlank(command.grantedByUserId())) {
            throw new ValidationException("email, fullName and grantedByUserId are required");
        }

        try {
            return userIdentityRepository.grantAccountantAccess(
                    command.tenantId(),
                    command.email().trim(),
                    command.fullName().trim(),
                    passwordHasher.hash(command.temporaryPassword() == null ? DEFAULT_TEMPORARY_PASSWORD : command.temporaryPassword()),
                    command.grantedByUserId()
            );
        } catch (RepositoryException exception) {
            throw new ValidationException(exception.getMessage());
        }
    }

    public List<UserView> listTenantMembers(String tenantId, String tenantHeader) {
        if (tenantHeader != null && !tenantHeader.equals(tenantId)) {
            throw new TenantMismatchException();
        }
        return userIdentityRepository.listTenantUsers(tenantId);
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

    private IdentityVerification toVerification(StoredUserCredentials credentials) {
        return new IdentityVerification(
                credentials.userId(),
                credentials.tenantId(),
                credentials.email(),
                credentials.fullName(),
                credentials.roles()
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
}
