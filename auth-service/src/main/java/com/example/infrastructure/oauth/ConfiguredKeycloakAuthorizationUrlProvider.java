package com.example.infrastructure.oauth;

import com.example.application.port.out.KeycloakAuthorizationUrlProvider;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@ApplicationScoped
public class ConfiguredKeycloakAuthorizationUrlProvider implements KeycloakAuthorizationUrlProvider {
    @ConfigProperty(name = "auth.keycloak.base-url")
    String baseUrl;

    @ConfigProperty(name = "auth.keycloak.public-base-url")
    Optional<String> publicBaseUrl;

    @ConfigProperty(name = "auth.keycloak.realm")
    String realm;

    @ConfigProperty(name = "auth.keycloak.client-id")
    String clientId;

    @ConfigProperty(name = "auth.keycloak.redirect-uri")
    String redirectUri;

    @ConfigProperty(name = "auth.keycloak.scope")
    String scope;

    @Override
    public Optional<String> authorizationUrl(String identityProviderHint) {
        if (isBlank(baseUrl) || isBlank(realm) || isBlank(clientId)
                || "not-configured".equals(baseUrl)
                || "not-configured".equals(realm)
                || "not-configured".equals(clientId)) {
            return Optional.empty();
        }

        String url = oidcEndpoint("auth")
                + "?client_id=" + encode(clientId)
                + "&redirect_uri=" + encode(redirectUri)
                + "&response_type=code"
                + "&scope=" + encode(scope);
        if (!isBlank(identityProviderHint)) {
            url += "&kc_idp_hint=" + encode(identityProviderHint.trim());
        }
        return Optional.of(url);
    }

    private String oidcEndpoint(String endpoint) {
        String resolvedBaseUrl = publicBaseUrl
                .filter(value -> !isBlank(value) && !"not-configured".equals(value))
                .orElse(baseUrl);
        return trimTrailingSlash(resolvedBaseUrl)
                + "/realms/" + encode(realm)
                + "/protocol/openid-connect/" + endpoint;
    }

    private String trimTrailingSlash(String value) {
        String trimmed = value.trim();
        while (trimmed.endsWith("/")) {
            trimmed = trimmed.substring(0, trimmed.length() - 1);
        }
        return trimmed;
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }
}
