package com.example.infrastructure.oauth;

import com.example.application.port.out.GoogleAuthorizationUrlProvider;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

@ApplicationScoped
public class ConfiguredGoogleAuthorizationUrlProvider implements GoogleAuthorizationUrlProvider {
    @ConfigProperty(name = "auth.google.client-id")
    String googleClientId;

    @ConfigProperty(name = "auth.google.redirect-uri")
    String googleRedirectUri;

    @Override
    public Optional<String> authorizationUrl() {
        if (isBlank(googleClientId) || "not-configured".equals(googleClientId)) {
            return Optional.empty();
        }

        String url = "https://accounts.google.com/o/oauth2/v2/auth"
                + "?client_id=" + encode(googleClientId)
                + "&redirect_uri=" + encode(googleRedirectUri)
                + "&response_type=code"
                + "&scope=" + encode("openid email profile")
                + "&access_type=offline"
                + "&prompt=consent";
        return Optional.of(url);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    private String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }
}
