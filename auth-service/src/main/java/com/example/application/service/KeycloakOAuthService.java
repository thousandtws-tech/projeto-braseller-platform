package com.example.application.service;

import com.example.application.exception.FeatureNotConfiguredException;
import com.example.application.port.out.KeycloakAuthorizationUrlProvider;
import com.example.application.port.out.KeycloakOAuthClient;
import com.example.domain.model.AuthTokenSet;
import com.example.domain.model.KeycloakTokenResponse;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

@ApplicationScoped
public class KeycloakOAuthService {
    @Inject
    KeycloakAuthorizationUrlProvider keycloakAuthorizationUrlProvider;

    @Inject
    KeycloakOAuthClient keycloakOAuthClient;

    @Inject
    AuthenticationService authenticationService;

    public String googleAuthorizationUrl() {
        return keycloakAuthorizationUrlProvider.authorizationUrl("google")
                .orElseThrow(() -> new FeatureNotConfiguredException("keycloak_oauth_not_configured"));
    }

    public AuthTokenSet googleCallback(String code, String tenantName) {
        KeycloakTokenResponse tokenResponse = keycloakOAuthClient.exchangeCode(code);
        return authenticationService.loginOrRegisterWithKeycloak(
                keycloakOAuthClient.userInfo(tokenResponse.accessToken()),
                tenantName,
                tokenResponse
        );
    }
}
