package com.example.application.service;

import com.example.application.command.LoginCommand;
import com.example.application.command.RefreshTokenCommand;
import com.example.application.command.RegisterCommand;
import com.example.application.exception.AuthenticationException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.AuthIdentityRepository;
import com.example.application.port.out.RefreshTokenHasher;
import com.example.application.port.out.TokenIssuer;
import com.example.application.port.out.UserIdentityGateway;
import com.example.domain.model.AuthIdentity;
import com.example.domain.model.AuthSession;
import com.example.domain.model.AuthTokenSet;
import com.example.domain.model.IssuedTokens;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.UUID;

@ApplicationScoped
public class AuthenticationService {
    @Inject
    UserIdentityGateway userIdentityGateway;

    @Inject
    AuthIdentityRepository authIdentityRepository;

    @Inject
    TokenIssuer tokenIssuer;

    @Inject
    RefreshTokenHasher refreshTokenHasher;

    public AuthTokenSet register(RegisterCommand command) {
        if (isBlank(command.tenantName()) || isBlank(command.fullName()) || isBlank(command.email()) || isWeakPassword(command.password())) {
            throw new ValidationException("tenantName, fullName, email and a password with at least 8 characters are required");
        }

        AuthIdentity identity = userIdentityGateway.registerTenant(command);
        return issueSession(authIdentityRepository.synchronize(identity));
    }

    public AuthTokenSet login(LoginCommand command) {
        if (isBlank(command.email()) || isBlank(command.password())) {
            throw new ValidationException("email and password are required");
        }

        AuthIdentity identity = userIdentityGateway.verifyPassword(command)
                .orElseThrow(() -> new AuthenticationException("invalid_credentials"));
        return issueSession(authIdentityRepository.synchronize(identity));
    }

    public AuthTokenSet refresh(RefreshTokenCommand command) {
        if (isBlank(command.refreshToken())) {
            throw new ValidationException("refreshToken is required");
        }

        String refreshHash = refreshTokenHasher.hash(command.refreshToken());
        AuthIdentity identity = authIdentityRepository.findIdentityByRefreshTokenHash(refreshHash)
                .orElseThrow(() -> new AuthenticationException("invalid_refresh_token"));
        return issueSession(identity);
    }

    public boolean logout(RefreshTokenCommand command) {
        if (isBlank(command.refreshToken())) {
            throw new ValidationException("refreshToken is required");
        }

        return authIdentityRepository.revokeRefreshToken(refreshTokenHasher.hash(command.refreshToken()));
    }

    private AuthTokenSet issueSession(AuthIdentity identity) {
        IssuedTokens issuedTokens = tokenIssuer.issue(identity);
        authIdentityRepository.createSession(new AuthSession(
                UUID.randomUUID().toString(),
                identity.tenantId(),
                identity.userId(),
                issuedTokens.tokenId(),
                refreshTokenHasher.hash(issuedTokens.refreshToken()),
                issuedTokens.refreshExpiresAt()
        ));
        return new AuthTokenSet(
                issuedTokens.accessToken(),
                issuedTokens.refreshToken(),
                "Bearer",
                issuedTokens.accessExpiresAt().toString(),
                identity.tenantId(),
                identity.userId(),
                identity.email(),
                identity.roles()
        );
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private boolean isWeakPassword(String value) {
        return value == null || value.length() < 8;
    }
}
