package com.example.application.service;

import com.example.application.command.LoginCommand;
import com.example.application.command.RefreshTokenCommand;
import com.example.application.command.RegisterCommand;
import com.example.application.command.SyncExternalProfileCommand;
import com.example.application.exception.AuthenticationException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.AuthIdentityRepository;
import com.example.application.port.out.KeycloakOAuthClient;
import com.example.application.port.out.TokenIssuer;
import com.example.application.port.out.UserIdentityGateway;
import com.example.domain.model.AuthIdentity;
import com.example.domain.model.AuthProfile;
import com.example.domain.model.AuthTokenSet;
import com.example.domain.model.IssuedTokens;
import com.example.domain.model.KeycloakIdentity;
import com.example.domain.model.KeycloakTokenResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

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
    KeycloakOAuthClient keycloakOAuthClient;

    @ConfigProperty(name = "auth.keycloak.require-email-verified")
    boolean requireKeycloakEmailVerified;

    public AuthTokenSet register(RegisterCommand command) {
        if (isBlank(command.tenantName()) || isBlank(command.fullName()) || isBlank(command.email()) || isWeakPassword(command.password())) {
            throw new ValidationException("tenantName, fullName, email and a password with at least 8 characters are required");
        }

        AuthIdentity identity = authIdentityRepository.synchronize(userIdentityGateway.registerTenant(command));
        keycloakOAuthClient.createPasswordUser(identity, command.password());
        KeycloakTokenResponse tokenResponse = keycloakOAuthClient.passwordGrant(identity.email(), command.password());
        KeycloakIdentity keycloakIdentity = keycloakOAuthClient.userInfo(tokenResponse.accessToken());
        return finishKeycloakSession(identity, keycloakIdentity, tokenResponse);
    }

    public AuthTokenSet login(LoginCommand command) {
        if (isBlank(command.email()) || isBlank(command.password())) {
            throw new ValidationException("email and password are required");
        }

        KeycloakTokenResponse tokenResponse = keycloakOAuthClient.passwordGrant(command.email(), command.password());
        KeycloakIdentity keycloakIdentity = keycloakOAuthClient.userInfo(tokenResponse.accessToken());
        AuthIdentity identity = authIdentityRepository.findIdentityByEmail(keycloakIdentity.email())
                .orElseThrow(() -> new AuthenticationException("invalid_credentials"));
        return finishKeycloakSession(identity, keycloakIdentity, tokenResponse);
    }

    public AuthTokenSet loginOrRegisterWithKeycloak(KeycloakIdentity keycloakIdentity, String tenantName,
                                                    KeycloakTokenResponse tokenResponse) {
        if (keycloakIdentity == null || isBlank(keycloakIdentity.email()) || isBlank(keycloakIdentity.subject())) {
            throw new AuthenticationException("invalid_keycloak_identity");
        }
        if (requireKeycloakEmailVerified && !keycloakIdentity.emailVerified()) {
            throw new AuthenticationException("keycloak_email_not_verified");
        }

        AuthIdentity identity = authIdentityRepository.findIdentityByEmail(keycloakIdentity.email())
                .orElseGet(() -> registerKeycloakIdentity(keycloakIdentity, tenantName));
        return finishKeycloakSession(identity, keycloakIdentity, tokenResponse);
    }

    private AuthIdentity registerKeycloakIdentity(KeycloakIdentity keycloakIdentity, String tenantName) {
        if (isBlank(tenantName)) {
            throw new ValidationException("tenantName is required for Keycloak signup");
        }
        AuthIdentity identity = userIdentityGateway.registerTenant(new RegisterCommand(
                tenantName.trim(),
                isBlank(keycloakIdentity.fullName()) ? keycloakIdentity.email() : keycloakIdentity.fullName(),
                keycloakIdentity.email(),
                generatedExternalPassword()
        ));
        return authIdentityRepository.synchronize(identity);
    }

    public AuthTokenSet refresh(RefreshTokenCommand command) {
        if (isBlank(command.refreshToken())) {
            throw new ValidationException("refreshToken is required");
        }

        KeycloakTokenResponse tokenResponse = keycloakOAuthClient.refresh(command.refreshToken());
        KeycloakIdentity keycloakIdentity = keycloakOAuthClient.userInfo(tokenResponse.accessToken());
        AuthIdentity identity = authIdentityRepository.findIdentityByEmail(keycloakIdentity.email())
                .orElseThrow(() -> new AuthenticationException("invalid_refresh_token"));
        String refreshToken = isBlank(tokenResponse.refreshToken()) ? command.refreshToken() : tokenResponse.refreshToken();
        AuthIdentity synchronizedIdentity = synchronizeKeycloakProfile(identity, keycloakIdentity);
        return issueKeycloakSession(
                synchronizedIdentity,
                keycloakProfile(synchronizedIdentity, keycloakIdentity),
                refreshToken
        );
    }

    public boolean logout(RefreshTokenCommand command) {
        if (isBlank(command.refreshToken())) {
            throw new ValidationException("refreshToken is required");
        }

        return keycloakOAuthClient.logout(command.refreshToken());
    }

    private AuthTokenSet finishKeycloakSession(AuthIdentity identity, KeycloakIdentity keycloakIdentity,
                                               KeycloakTokenResponse tokenResponse) {
        AuthIdentity synchronizedIdentity = synchronizeKeycloakProfile(identity, keycloakIdentity);
        authIdentityRepository.linkProviderSubject(synchronizedIdentity.email(), "KEYCLOAK", keycloakIdentity.subject());
        keycloakOAuthClient.synchronizeUser(synchronizedIdentity, keycloakIdentity.subject());
        return issueKeycloakSession(
                synchronizedIdentity,
                keycloakProfile(synchronizedIdentity, keycloakIdentity),
                tokenResponse.refreshToken()
        );
    }

    private AuthTokenSet issueKeycloakSession(AuthIdentity identity, AuthProfile profile, String keycloakRefreshToken) {
        IssuedTokens issuedTokens = tokenIssuer.issue(identity);
        return new AuthTokenSet(
                issuedTokens.accessToken(),
                keycloakRefreshToken,
                "Bearer",
                issuedTokens.accessExpiresAt().toString(),
                identity.tenantId(),
                identity.userId(),
                identity.email(),
                identity.roles(),
                profile
        );
    }

    private AuthProfile keycloakProfile(AuthIdentity identity, KeycloakIdentity keycloakIdentity) {
        return new AuthProfile(
                "KEYCLOAK",
                keycloakIdentity.subject(),
                identity.tenantId(),
                identity.userId(),
                identity.email(),
                isBlank(keycloakIdentity.fullName()) ? identity.fullName() : keycloakIdentity.fullName(),
                isBlank(keycloakIdentity.preferredUsername()) ? identity.email() : keycloakIdentity.preferredUsername(),
                keycloakIdentity.firstName(),
                keycloakIdentity.lastName(),
                keycloakIdentity.pictureUrl(),
                keycloakIdentity.emailVerified(),
                identity.roles()
        );
    }

    private AuthIdentity synchronizeKeycloakProfile(AuthIdentity fallback, KeycloakIdentity keycloakIdentity) {
        return userIdentityGateway.syncExternalProfile(new SyncExternalProfileCommand(
                        keycloakIdentity.email(),
                        "KEYCLOAK",
                        keycloakIdentity.subject(),
                        keycloakIdentity.fullName(),
                        keycloakIdentity.preferredUsername(),
                        keycloakIdentity.firstName(),
                        keycloakIdentity.lastName(),
                        keycloakIdentity.pictureUrl(),
                        keycloakIdentity.emailVerified()
                ))
                .map(authIdentityRepository::synchronize)
                .orElse(fallback);
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
