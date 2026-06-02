package com.example.infrastructure.oauth;

import com.example.application.exception.AuthenticationException;
import com.example.application.exception.FeatureNotConfiguredException;
import com.example.application.port.out.KeycloakOAuthClient;
import com.example.domain.model.AuthIdentity;
import com.example.domain.model.KeycloakIdentity;
import com.example.domain.model.KeycloakTokenResponse;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.StringJoiner;

@ApplicationScoped
public class HttpKeycloakOAuthClient implements KeycloakOAuthClient {
    private HttpClient httpClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "auth.keycloak.base-url")
    String baseUrl;

    @ConfigProperty(name = "auth.keycloak.realm")
    String realm;

    @ConfigProperty(name = "auth.keycloak.client-id")
    String clientId;

    @ConfigProperty(name = "auth.keycloak.client-secret")
    String clientSecret;

    @ConfigProperty(name = "auth.keycloak.redirect-uri")
    String redirectUri;

    @ConfigProperty(name = "auth.keycloak.scope")
    String scope;

    @ConfigProperty(name = "auth.keycloak.admin-username")
    String adminUsername;

    @ConfigProperty(name = "auth.keycloak.admin-password")
    String adminPassword;

    @ConfigProperty(name = "auth.http-client.connect-timeout-ms")
    long connectTimeoutMs;

    @ConfigProperty(name = "auth.http-client.request-timeout-ms")
    long requestTimeoutMs;

    @PostConstruct
    void initHttpClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    @Override
    public KeycloakTokenResponse exchangeCode(String code) {
        requireConfigured();
        if (isBlank(code)) {
            throw new AuthenticationException("keycloak_code_required");
        }

        Map<String, String> formValues = clientForm();
        formValues.put("grant_type", "authorization_code");
        formValues.put("code", code);
        formValues.put("redirect_uri", redirectUri);
        addScope(formValues);
        HttpResponse<String> response = postForm(oidcEndpoint(realm, "token"), formValues);
        if (response.statusCode() != 200) {
            throw new AuthenticationException("keycloak_token_exchange_failed");
        }
        return readTokenResponse(response.body(), "keycloak_token_exchange_unavailable");
    }

    @Override
    public KeycloakTokenResponse passwordGrant(String email, String password) {
        requireConfigured();
        if (isBlank(email) || isBlank(password)) {
            throw new AuthenticationException("invalid_credentials");
        }

        Map<String, String> formValues = clientForm();
        formValues.put("grant_type", "password");
        formValues.put("username", email.trim());
        formValues.put("password", password);
        addScope(formValues);
        HttpResponse<String> response = postForm(oidcEndpoint(realm, "token"), formValues);
        if (response.statusCode() == 400 || response.statusCode() == 401) {
            throw new AuthenticationException("invalid_credentials");
        }
        if (response.statusCode() != 200) {
            throw new AuthenticationException("keycloak_password_grant_failed");
        }
        return readTokenResponse(response.body(), "keycloak_password_grant_unavailable");
    }

    @Override
    public KeycloakTokenResponse refresh(String refreshToken) {
        requireConfigured();
        if (isBlank(refreshToken)) {
            throw new AuthenticationException("invalid_refresh_token");
        }

        Map<String, String> formValues = clientForm();
        formValues.put("grant_type", "refresh_token");
        formValues.put("refresh_token", refreshToken);
        addScope(formValues);
        HttpResponse<String> response = postForm(oidcEndpoint(realm, "token"), formValues);
        if (response.statusCode() == 400 || response.statusCode() == 401) {
            throw new AuthenticationException("invalid_refresh_token");
        }
        if (response.statusCode() != 200) {
            throw new AuthenticationException("keycloak_refresh_failed");
        }
        return readTokenResponse(response.body(), "keycloak_refresh_unavailable");
    }

    @Override
    public boolean logout(String refreshToken) {
        requireConfigured();
        if (isBlank(refreshToken)) {
            throw new AuthenticationException("invalid_refresh_token");
        }

        Map<String, String> formValues = clientForm();
        formValues.put("refresh_token", refreshToken);
        HttpResponse<String> response = postForm(oidcEndpoint(realm, "logout"), formValues);
        if (response.statusCode() == 400 || response.statusCode() == 401) {
            return false;
        }
        if (response.statusCode() == 200 || response.statusCode() == 204) {
            return true;
        }
        throw new AuthenticationException("keycloak_logout_failed");
    }

    @Override
    public KeycloakIdentity userInfo(String accessToken) {
        requireConfigured();
        if (isBlank(accessToken)) {
            throw new AuthenticationException("keycloak_access_token_required");
        }

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(oidcEndpoint(realm, "userinfo")))
                    .timeout(requestTimeout())
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + accessToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AuthenticationException("keycloak_userinfo_unavailable");
            }
            Map<String, Object> claims = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            String subject = stringClaim(claims, "sub");
            String email = stringClaim(claims, "email");
            String fullName = firstPresent(claims, "name", "preferred_username", "given_name");
            String preferredUsername = firstPresent(claims, "preferred_username");
            String firstName = firstPresent(claims, "given_name");
            String lastName = firstPresent(claims, "family_name");
            String pictureUrl = firstPresent(claims, "picture");
            boolean emailVerified = Boolean.parseBoolean(String.valueOf(claims.getOrDefault("email_verified", "false")));
            return new KeycloakIdentity(
                    subject,
                    email,
                    isBlank(fullName) ? email : fullName,
                    emailVerified,
                    preferredUsername,
                    firstName,
                    lastName,
                    pictureUrl
            );
        } catch (AuthenticationException | FeatureNotConfiguredException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("keycloak_userinfo_unavailable");
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthenticationException("keycloak_userinfo_unavailable");
        }
    }

    @Override
    public void createPasswordUser(AuthIdentity identity, String password) {
        requireConfigured();
        requireAdminConfigured();
        if (identity == null || isBlank(identity.email()) || isBlank(password)) {
            throw new AuthenticationException("keycloak_user_create_failed");
        }

        String adminToken = adminToken();
        Optional<AdminUser> current = findUserByEmail(adminToken, identity.email());
        if (current.isPresent()) {
            updateUser(adminToken, current.get().id(), identity, current.get().id());
            resetPassword(adminToken, current.get().id(), password);
            return;
        }

        HttpResponse<String> response = postJson(adminEndpoint("/users"), adminToken, userPayload(identity, password, null));
        if (response.statusCode() == 409) {
            AdminUser existing = findUserByEmail(adminToken, identity.email())
                    .orElseThrow(() -> new AuthenticationException("keycloak_user_create_failed"));
            updateUser(adminToken, existing.id(), identity, existing.id());
            resetPassword(adminToken, existing.id(), password);
            return;
        }
        if (response.statusCode() != 201 && response.statusCode() != 204) {
            throw new AuthenticationException("keycloak_user_create_failed");
        }
    }

    @Override
    public void synchronizeUser(AuthIdentity identity, String keycloakSubject) {
        requireConfigured();
        requireAdminConfigured();
        if (identity == null || isBlank(identity.email())) {
            throw new AuthenticationException("keycloak_user_sync_failed");
        }

        String adminToken = adminToken();
        Optional<AdminUser> current = findUserByEmail(adminToken, identity.email());
        if (current.isPresent()) {
            updateUser(adminToken, current.get().id(), identity, keycloakSubject);
            return;
        }

        HttpResponse<String> response = postJson(adminEndpoint("/users"), adminToken, userPayload(identity, null, keycloakSubject));
        if (response.statusCode() != 201 && response.statusCode() != 204 && response.statusCode() != 409) {
            throw new AuthenticationException("keycloak_user_sync_failed");
        }
    }

    private KeycloakTokenResponse readTokenResponse(String body, String failureMessage) {
        try {
            TokenResponse tokenResponse = objectMapper.readValue(body, TokenResponse.class);
            return new KeycloakTokenResponse(
                    tokenResponse.accessToken(),
                    tokenResponse.idToken(),
                    tokenResponse.refreshToken(),
                    tokenResponse.tokenType(),
                    tokenResponse.expiresIn()
            );
        } catch (IOException exception) {
            throw new AuthenticationException(failureMessage);
        }
    }

    private HttpResponse<String> postForm(String url, Map<String, String> formValues) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(requestTimeout())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/x-www-form-urlencoded")
                    .POST(HttpRequest.BodyPublishers.ofString(form(formValues)))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("keycloak_unavailable");
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthenticationException("keycloak_unavailable");
        }
    }

    private HttpResponse<String> postJson(String url, String bearerToken, Object body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(requestTimeout())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("keycloak_admin_unavailable");
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthenticationException("keycloak_admin_unavailable");
        }
    }

    private HttpResponse<String> putJson(String url, String bearerToken, Object body) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .timeout(requestTimeout())
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + bearerToken)
                    .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
            return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("keycloak_admin_unavailable");
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthenticationException("keycloak_admin_unavailable");
        }
    }

    private String adminToken() {
        Map<String, String> formValues = new LinkedHashMap<>();
        formValues.put("grant_type", "password");
        formValues.put("client_id", "admin-cli");
        formValues.put("username", adminUsername);
        formValues.put("password", adminPassword);
        HttpResponse<String> response = postForm(oidcEndpoint("master", "token"), formValues);
        if (response.statusCode() != 200) {
            throw new AuthenticationException("keycloak_admin_unavailable");
        }
        return readTokenResponse(response.body(), "keycloak_admin_unavailable").accessToken();
    }

    private Optional<AdminUser> findUserByEmail(String adminToken, String email) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(adminEndpoint("/users?email=" + encode(email.trim()) + "&exact=true")))
                    .timeout(requestTimeout())
                    .header("Accept", "application/json")
                    .header("Authorization", "Bearer " + adminToken)
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new AuthenticationException("keycloak_user_lookup_failed");
            }
            List<AdminUser> users = objectMapper.readValue(response.body(), new TypeReference<>() {
            });
            return users.stream()
                    .filter(user -> email.equalsIgnoreCase(user.email()))
                    .findFirst();
        } catch (AuthenticationException exception) {
            throw exception;
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AuthenticationException("keycloak_user_lookup_failed");
        } catch (IOException | IllegalArgumentException exception) {
            throw new AuthenticationException("keycloak_user_lookup_failed");
        }
    }

    private void updateUser(String adminToken, String userId, AuthIdentity identity, String keycloakSubject) {
        HttpResponse<String> response = putJson(adminEndpoint("/users/" + encode(userId)), adminToken,
                userPayload(identity, null, keycloakSubject));
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new AuthenticationException("keycloak_user_sync_failed");
        }
    }

    private void resetPassword(String adminToken, String userId, String password) {
        Map<String, Object> credential = new LinkedHashMap<>();
        credential.put("type", "password");
        credential.put("value", password);
        credential.put("temporary", false);
        HttpResponse<String> response = putJson(adminEndpoint("/users/" + encode(userId) + "/reset-password"), adminToken, credential);
        if (response.statusCode() != 200 && response.statusCode() != 204) {
            throw new AuthenticationException("keycloak_user_password_sync_failed");
        }
    }

    private Map<String, Object> userPayload(AuthIdentity identity, String password, String keycloakSubject) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", identity.email());
        payload.put("email", identity.email());
        payload.put("firstName", firstName(identity));
        payload.put("lastName", lastName(identity));
        payload.put("enabled", true);
        payload.put("emailVerified", identity.emailVerified());
        payload.put("attributes", attributes(identity, keycloakSubject));
        if (!isBlank(password)) {
            payload.put("credentials", List.of(Map.of(
                    "type", "password",
                    "value", password,
                    "temporary", false
            )));
        }
        return payload;
    }

    private Map<String, List<String>> attributes(AuthIdentity identity, String keycloakSubject) {
        Map<String, List<String>> attributes = new LinkedHashMap<>();
        attributes.put("tenant_id", List.of(identity.tenantId()));
        attributes.put("user_id", List.of(identity.userId()));
        attributes.put("roles", identity.roles());
        attributes.put("full_name", List.of(identity.fullName()));
        if (!isBlank(keycloakSubject)) {
            attributes.put("keycloak_subject", List.of(keycloakSubject));
            attributes.put("provider_subject", List.of(keycloakSubject));
        }
        return attributes;
    }

    private Map<String, String> clientForm() {
        Map<String, String> formValues = new LinkedHashMap<>();
        formValues.put("client_id", clientId);
        if (!isBlank(clientSecret) && !"not-configured".equals(clientSecret)) {
            formValues.put("client_secret", clientSecret);
        }
        return formValues;
    }

    private void addScope(Map<String, String> formValues) {
        if (!isBlank(scope)) {
            formValues.put("scope", scope);
        }
    }

    private void requireConfigured() {
        if (isBlank(baseUrl) || isBlank(realm) || isBlank(clientId)
                || "not-configured".equals(baseUrl)
                || "not-configured".equals(realm)
                || "not-configured".equals(clientId)) {
            throw new FeatureNotConfiguredException("keycloak_oauth_not_configured");
        }
    }

    private void requireAdminConfigured() {
        if (isBlank(adminUsername) || isBlank(adminPassword)
                || "not-configured".equals(adminUsername)
                || "not-configured".equals(adminPassword)) {
            throw new FeatureNotConfiguredException("keycloak_admin_not_configured");
        }
    }

    private String oidcEndpoint(String endpointRealm, String endpoint) {
        return trimTrailingSlash(baseUrl)
                + "/realms/" + encode(endpointRealm)
                + "/protocol/openid-connect/" + endpoint;
    }

    private String adminEndpoint(String path) {
        return trimTrailingSlash(baseUrl)
                + "/admin/realms/" + encode(realm)
                + path;
    }

    private String firstPresent(Map<String, Object> claims, String... names) {
        for (String name : names) {
            Object value = claims.get(name);
            if (value != null && !value.toString().isBlank()) {
                return value.toString();
            }
        }
        return null;
    }

    private String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value == null || value.toString().isBlank()) {
            throw new AuthenticationException("missing_keycloak_" + name);
        }
        return value.toString();
    }

    private String firstName(AuthIdentity identity) {
        if (!isBlank(identity.firstName())) {
            return identity.firstName();
        }
        if (isBlank(identity.fullName())) {
            return identity.email();
        }
        return identity.fullName().trim().split("\\s+", 2)[0];
    }

    private String lastName(AuthIdentity identity) {
        if (!isBlank(identity.lastName())) {
            return identity.lastName();
        }
        if (isBlank(identity.fullName()) || !identity.fullName().trim().contains(" ")) {
            return "";
        }
        return identity.fullName().trim().split("\\s+", 2)[1];
    }

    private String form(Map<String, String> values) {
        StringJoiner joiner = new StringJoiner("&");
        values.forEach((name, value) -> joiner.add(encode(name) + "=" + encode(value)));
        return joiner.toString();
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

    private Duration requestTimeout() {
        return Duration.ofMillis(requestTimeoutMs);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record TokenResponse(
            @JsonProperty("access_token") String accessToken,
            @JsonProperty("id_token") String idToken,
            @JsonProperty("refresh_token") String refreshToken,
            @JsonProperty("token_type") String tokenType,
            @JsonProperty("expires_in") long expiresIn) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AdminUser(String id, String username, String email) {
    }
}
