package com.example.application.service;

import com.example.infrastructure.client.PluggyAuthRequest;
import com.example.infrastructure.client.PluggyConnectTokenRequest;
import com.example.infrastructure.client.PluggyRestClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;

@ApplicationScoped
public class PluggyConnectService {
    @ConfigProperty(name = "pluggy.client-id")
    String clientId;

    @ConfigProperty(name = "pluggy.client-secret")
    String clientSecret;

    @RestClient
    @Inject
    PluggyRestClient pluggyRestClient;

    public String createConnectToken(String tenantId, String clientUserId) {
        if (isBlank(clientId) || isBlank(clientSecret)) {
            throw new WebApplicationException("pluggy_credentials_not_configured", 503);
        }

        String resolvedClientUserId = isBlank(clientUserId) ? tenantId : clientUserId.trim();
        String apiKey = pluggyRestClient.authenticate(new PluggyAuthRequest(clientId, clientSecret)).apiKey();
        if (isBlank(apiKey)) {
            throw new WebApplicationException("pluggy_auth_failed", 502);
        }

        String accessToken = pluggyRestClient
                .createConnectToken(apiKey, new PluggyConnectTokenRequest(resolvedClientUserId))
                .accessToken();
        if (isBlank(accessToken)) {
            throw new WebApplicationException("pluggy_connect_token_failed", 502);
        }

        return accessToken;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
