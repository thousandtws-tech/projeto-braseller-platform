package com.example.infrastructure.security;

import com.example.application.exception.ForbiddenException;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@ApplicationScoped
public class ConfiguredBillingWebhookAuthorizer {
    @ConfigProperty(name = "billing.webhook-token")
    String expectedToken;

    public void requireWebhookToken(String providedToken) {
        if (providedToken == null || providedToken.isBlank()
                || !MessageDigest.isEqual(bytes(expectedToken), bytes(providedToken))) {
            throw new ForbiddenException("invalid_billing_webhook_token");
        }
    }

    private byte[] bytes(String value) {
        return value == null ? new byte[0] : value.getBytes(StandardCharsets.UTF_8);
    }
}
