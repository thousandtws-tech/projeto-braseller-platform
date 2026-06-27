package com.example.application.service;

import java.util.UUID;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import com.example.application.command.LoginCommand;
import com.example.application.command.RefreshTokenCommand;
import com.example.application.command.RegisterCommand;
import com.example.application.exception.AuthenticationException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.AuthIdentityRepository;
import com.example.application.port.out.TokenIssuer;
import com.example.application.port.out.UserIdentityGateway;
import com.example.domain.model.AuthIdentity;
import com.example.domain.model.AuthProfile;
import com.example.domain.model.AuthTokenSet;
import com.example.domain.model.EmailVerificationDispatch;
import com.example.domain.model.EmailVerificationResult;
import com.example.domain.model.IssuedTokens;
import com.example.domain.model.RegistrationResult;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class AuthenticationService {
    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_PENDING_EMAIL_VERIFICATION = "PENDING_EMAIL_VERIFICATION";

    @Inject
    UserIdentityGateway userIdentityGateway;

    @Inject
    AuthIdentityRepository authIdentityRepository;

    @Inject
    TokenIssuer tokenIssuer;

    public RegistrationResult register(RegisterCommand command) {
        if (isBlank(command.tenantName()) || isBlank(command.fullName()) || isBlank(command.email())
                || isWeakPassword(command.password())) {
            throw new ValidationException(
                    "tenantName, fullName, email and a password with at least 8 characters are required");
        }

        AuthIdentity identity = authIdentityRepository.synchronize(userIdentityGateway.registerTenant(command));
        EmailVerificationDispatch verificationDispatch = userIdentityGateway.resendEmailVerificationCode(identity.email());
        return new RegistrationResult(
                identity.email(),
                identity.status(),
                true,
                verificationDispatch.expiresAt()
        );
    }

    public AuthTokenSet login(LoginCommand command) {
        if (isBlank(command.email()) || isBlank(command.password())) {
            throw new ValidationException("email and password are required");
        }

        AuthIdentity identity = userIdentityGateway.verifyPassword(command)
                .map(authIdentityRepository::synchronize)
                .orElseThrow(() -> new AuthenticationException("invalid_credentials"));

        ensureEmailVerified(identity);

        return issueSession(identity, localProfile(identity));
    }

    public EmailVerificationResult verifyEmailCode(String email, String code) {
        if (isBlank(email) || isBlank(code)) {
            throw new ValidationException("email and code are required");
        }

        AuthIdentity verifiedIdentity = authIdentityRepository.synchronize(userIdentityGateway.verifyEmailCode(email, code));
        return new EmailVerificationResult(verifiedIdentity.email(), verifiedIdentity.status(), verifiedIdentity.emailVerified());
    }

    public EmailVerificationDispatch resendEmailVerificationCode(String email) {
        if (isBlank(email)) {
            throw new ValidationException("email is required");
        }
        return userIdentityGateway.resendEmailVerificationCode(email);
    }

    public AuthTokenSet refresh(RefreshTokenCommand command) {
        if (isBlank(command.refreshToken())) {
            throw new ValidationException("refreshToken is required");
        }

        // Simplificacao: para JWT original Quarkus sem Keycloak,
        // teriamos que gerenciar refresh tokens localmente se quisermos manter a funcionalidade.
        // Por agora, vamos retornar erro ou implementar um mock basico se o objetivo e apenas remover o Keycloak.
        // O Quarkus Security nativo costuma usar apenas Access Tokens (JWT) ou Session Cookies.
        throw new AuthenticationException("refresh_not_implemented_locally");
    }

    public boolean logout(RefreshTokenCommand command) {
        return true;
    }

    private AuthTokenSet issueSession(AuthIdentity identity, AuthProfile profile) {
        IssuedTokens issuedTokens = tokenIssuer.issue(identity);
        return new AuthTokenSet(
                issuedTokens.accessToken(),
                null,
                "Bearer",
                issuedTokens.accessExpiresAt().toString(),
                identity.tenantId(),
                identity.userId(),
                identity.email(),
                identity.roles(),
                profile);
    }

    private AuthProfile localProfile(AuthIdentity identity) {
        return new AuthProfile(
                "LOCAL",
                identity.userId(),
                identity.tenantId(),
                identity.userId(),
                identity.email(),
                identity.fullName(),
                identity.email(),
                identity.fullName(),
                "",
                null,
                identity.emailVerified(),
                identity.roles());
    }

    private void ensureEmailVerified(AuthIdentity identity) {
        if (identity == null) {
            throw new AuthenticationException("invalid_credentials");
        }
        if (!identity.emailVerified() || STATUS_PENDING_EMAIL_VERIFICATION.equals(identity.status())) {
            throw new AuthenticationException("email_verification_required");
        }
        if (!STATUS_ACTIVE.equals(identity.status())) {
            throw new AuthenticationException("account_not_active");
        }
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isWeakPassword(String value) {
        return value == null || value.length() < 8;
    }

    private String generatedExternalPassword() {
        return UUID.randomUUID() + "Aa1!";
    }
}
