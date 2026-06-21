package com.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserServiceMockResource implements QuarkusTestResourceLifecycleManager {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String INTERNAL_TOKEN = "test-internal-token";
    private static final String ADMIN_TOKEN = "test-admin-token";
    private static final Map<String, StoredIdentity> IDENTITIES = new ConcurrentHashMap<>();
    private static final Map<String, KeycloakUser> KEYCLOAK_USERS_BY_EMAIL = new ConcurrentHashMap<>();
    private static final Map<String, KeycloakUser> KEYCLOAK_USERS_BY_ID = new ConcurrentHashMap<>();
    private static final Map<String, KeycloakUser> ACCESS_TOKENS = new ConcurrentHashMap<>();
    private static final Map<String, KeycloakUser> REFRESH_TOKENS = new ConcurrentHashMap<>();

    private HttpServer server;

    @Override
    public Map<String, String> start() {
        try {
            clearState();
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/users/tenants/register", this::handleRegister);
            server.createContext("/users/internal/identity/verify-password", this::handleVerifyPassword);
            server.createContext("/users/internal/identity/by-email", this::handleFindByEmail);
            server.createContext("/users/internal/identity/mark-email-verified", this::handleMarkEmailVerified);
            server.createContext("/users/internal/identity/reset-password", this::handleResetPassword);
            server.createContext("/notifications/events/auth-email", this::handleAuthEmail);
            server.createContext("/users/internal/identity/sync-profile", this::handleSyncProfile);
            server.createContext("/realms", this::handleKeycloakRealm);
            server.createContext("/admin/realms/brasaller/users", this::handleKeycloakAdminUsers);
            server.start();
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            return Map.ofEntries(
                    Map.entry("auth.user-service.url", baseUrl),
                    Map.entry("auth.user-service.internal-token", INTERNAL_TOKEN),
                    Map.entry("auth.keycloak.base-url", baseUrl),
                    Map.entry("auth.keycloak.public-base-url", baseUrl),
                    Map.entry("auth.keycloak.realm", "brasaller"),
                    Map.entry("auth.keycloak.client-id", "auth-service"),
                    Map.entry("auth.keycloak.client-secret", "not-configured"),
                    Map.entry("auth.keycloak.redirect-uri", "http://localhost:3000/auth/callback"),
                    Map.entry("auth.keycloak.admin-username", "admin"),
                    Map.entry("auth.keycloak.admin-password", "admin"),
                    Map.entry("auth.notification-service.url", baseUrl),
                    Map.entry("auth.notification-service.internal-token", INTERNAL_TOKEN)
            );
        } catch (IOException exception) {
            throw new IllegalStateException(exception);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
        clearState();
    }

    private void clearState() {
        IDENTITIES.clear();
        KEYCLOAK_USERS_BY_EMAIL.clear();
        KEYCLOAK_USERS_BY_ID.clear();
        ACCESS_TOKENS.clear();
        REFRESH_TOKENS.clear();
    }

    private void handleRegister(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("message", "method_not_allowed"));
            return;
        }

        Map<String, String> request = OBJECT_MAPPER.readValue(exchange.getRequestBody(), new TypeReference<>() {
        });
        String email = request.get("email");
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        if (IDENTITIES.containsKey(normalizedEmail)) {
            send(exchange, 409, Map.of("message", "Could not register tenant"));
            return;
        }

        StoredIdentity identity = new StoredIdentity(
                UUID.randomUUID().toString(),
                UUID.randomUUID().toString(),
                email,
                request.get("adminName"),
                email,
                null,
                null,
                null,
                false,
                "PASSWORD",
                null,
                request.get("password"),
                List.of("ADMIN", "VENDEDOR"),
                "PENDING_EMAIL_VERIFICATION"
        );
        IDENTITIES.put(normalizedEmail, identity);

        send(exchange, 201, Map.of(
                "tenant", Map.of(
                        "id", identity.tenantId(),
                        "legalName", request.get("legalName"),
                        "tradeName", request.get("tradeName"),
                        "status", "ACTIVE"
                ),
                "adminUser", userResponse(identity)
        ));
    }

    private void handleVerifyPassword(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("message", "method_not_allowed"));
            return;
        }
        if (!INTERNAL_TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))) {
            send(exchange, 403, Map.of("message", "invalid_internal_token"));
            return;
        }

        Map<String, String> request = OBJECT_MAPPER.readValue(exchange.getRequestBody(), new TypeReference<>() {
        });
        StoredIdentity identity = IDENTITIES.get(request.get("email").toLowerCase(Locale.ROOT));
        if (identity == null || !identity.password().equals(request.get("password"))
                || !identity.emailVerified() || !"ACTIVE".equals(identity.status())) {
            send(exchange, 401, Map.of("message", "invalid_credentials"));
            return;
        }

        send(exchange, 200, verificationResponse(identity));
    }

    private void handleFindByEmail(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("message", "method_not_allowed"));
            return;
        }
        if (!INTERNAL_TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))) {
            send(exchange, 403, Map.of("message", "invalid_internal_token"));
            return;
        }

        Map<String, String> request = OBJECT_MAPPER.readValue(exchange.getRequestBody(), new TypeReference<>() {
        });
        StoredIdentity identity = IDENTITIES.get(request.get("email").toLowerCase(Locale.ROOT));
        if (identity == null) {
            send(exchange, 404, Map.of("message", "user_not_found"));
            return;
        }
        send(exchange, 200, userResponse(identity));
    }

    private void handleMarkEmailVerified(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("message", "method_not_allowed"));
            return;
        }
        if (!INTERNAL_TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))) {
            send(exchange, 403, Map.of("message", "invalid_internal_token"));
            return;
        }

        Map<String, String> request = OBJECT_MAPPER.readValue(exchange.getRequestBody(), new TypeReference<>() {
        });
        String normalizedEmail = request.get("email").toLowerCase(Locale.ROOT);
        StoredIdentity current = IDENTITIES.get(normalizedEmail);
        if (current == null) {
            send(exchange, 404, Map.of("message", "user_not_found"));
            return;
        }
        StoredIdentity updated = current.withEmailVerified(true).withStatus("ACTIVE");
        IDENTITIES.put(normalizedEmail, updated);
        send(exchange, 200, userResponse(updated));
    }

    private void handleResetPassword(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("message", "method_not_allowed"));
            return;
        }
        if (!INTERNAL_TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))) {
            send(exchange, 403, Map.of("message", "invalid_internal_token"));
            return;
        }

        Map<String, String> request = OBJECT_MAPPER.readValue(exchange.getRequestBody(), new TypeReference<>() {
        });
        String normalizedEmail = request.get("email").toLowerCase(Locale.ROOT);
        StoredIdentity current = IDENTITIES.get(normalizedEmail);
        if (current == null || !current.emailVerified() || !"ACTIVE".equals(current.status())) {
            send(exchange, 404, Map.of("message", "user_not_found"));
            return;
        }
        StoredIdentity updated = current.withPassword(request.get("newPassword"));
        IDENTITIES.put(normalizedEmail, updated);
        send(exchange, 200, userResponse(updated));
    }

    private void handleAuthEmail(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("message", "method_not_allowed"));
            return;
        }
        if (!INTERNAL_TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))) {
            send(exchange, 403, Map.of("message", "invalid_internal_token"));
            return;
        }
        exchange.getRequestBody().readAllBytes();
        send(exchange, 202, Map.of("reason", "sent"));
    }

    private void handleSyncProfile(HttpExchange exchange) throws IOException {
        if (!"POST".equals(exchange.getRequestMethod())) {
            send(exchange, 405, Map.of("message", "method_not_allowed"));
            return;
        }
        if (!INTERNAL_TOKEN.equals(exchange.getRequestHeaders().getFirst("X-Internal-Token"))) {
            send(exchange, 403, Map.of("message", "invalid_internal_token"));
            return;
        }

        Map<String, Object> request = OBJECT_MAPPER.readValue(exchange.getRequestBody(), new TypeReference<>() {
        });
        String email = String.valueOf(request.get("email"));
        String normalizedEmail = email.toLowerCase(Locale.ROOT);
        StoredIdentity current = IDENTITIES.get(normalizedEmail);
        if (current == null) {
            send(exchange, 404, Map.of("message", "user_not_found"));
            return;
        }

        boolean requestedEmailVerified = Boolean.parseBoolean(String.valueOf(request.getOrDefault("emailVerified", current.emailVerified())));
        StoredIdentity updated = new StoredIdentity(
                current.tenantId(),
                current.userId(),
                current.email(),
                stringValue(request.get("fullName"), current.fullName()),
                stringValue(request.get("preferredUsername"), current.preferredUsername()),
                stringValue(request.get("firstName"), current.firstName()),
                stringValue(request.get("lastName"), current.lastName()),
                stringValue(request.get("pictureUrl"), current.pictureUrl()),
                current.emailVerified() || requestedEmailVerified,
                stringValue(request.get("provider"), current.provider()),
                stringValue(request.get("providerSubject"), current.providerSubject()),
                current.password(),
                current.roles(),
                requestedEmailVerified ? "ACTIVE" : current.status()
        );
        IDENTITIES.put(normalizedEmail, updated);
        send(exchange, 200, userResponse(updated));
    }

    private void handleKeycloakRealm(HttpExchange exchange) throws IOException {
        String path = exchange.getRequestURI().getPath();
        if (path.equals("/realms/master/protocol/openid-connect/token")) {
            handleKeycloakAdminToken(exchange);
            return;
        }
        if (path.equals("/realms/brasaller/protocol/openid-connect/token")) {
            handleKeycloakToken(exchange);
            return;
        }
        if (path.equals("/realms/brasaller/protocol/openid-connect/userinfo")) {
            handleKeycloakUserInfo(exchange);
            return;
        }
        if (path.equals("/realms/brasaller/protocol/openid-connect/logout")) {
            handleKeycloakLogout(exchange);
            return;
        }
        send(exchange, 404, Map.of("message", "not_found"));
    }

    private void handleKeycloakAdminToken(HttpExchange exchange) throws IOException {
        Map<String, String> form = form(exchange);
        if (!"admin-cli".equals(form.get("client_id"))
                || !"admin".equals(form.get("username"))
                || !"admin".equals(form.get("password"))) {
            send(exchange, 401, Map.of("error", "invalid_grant"));
            return;
        }
        send(exchange, 200, tokenResponse(ADMIN_TOKEN, null));
    }

    private void handleKeycloakToken(HttpExchange exchange) throws IOException {
        Map<String, String> form = form(exchange);
        String grantType = form.get("grant_type");
        if ("password".equals(grantType)) {
            KeycloakUser user = KEYCLOAK_USERS_BY_EMAIL.get(normalize(form.get("username")));
            if (user == null || user.password() == null || !user.password().equals(form.get("password"))) {
                send(exchange, 401, Map.of("error", "invalid_grant"));
                return;
            }
            send(exchange, 200, issueTokens(user));
            return;
        }
        if ("refresh_token".equals(grantType)) {
            KeycloakUser user = REFRESH_TOKENS.get(form.get("refresh_token"));
            if (user == null) {
                send(exchange, 400, Map.of("error", "invalid_grant"));
                return;
            }
            send(exchange, 200, issueTokens(user));
            return;
        }
        if ("authorization_code".equals(grantType)) {
            KeycloakUser user = oauthUser(form.get("code"));
            if (user == null) {
                send(exchange, 400, Map.of("error", "invalid_grant"));
                return;
            }
            send(exchange, 200, issueTokens(user));
            return;
        }
        send(exchange, 400, Map.of("error", "unsupported_grant_type"));
    }

    private void handleKeycloakUserInfo(HttpExchange exchange) throws IOException {
        String authorization = exchange.getRequestHeaders().getFirst("Authorization");
        if (authorization == null || !authorization.startsWith("Bearer ")) {
            send(exchange, 401, Map.of("message", "missing_token"));
            return;
        }
        KeycloakUser user = ACCESS_TOKENS.get(authorization.substring("Bearer ".length()));
        if (user == null) {
            send(exchange, 401, Map.of("message", "invalid_token"));
            return;
        }
        send(exchange, 200, Map.of(
                "sub", user.id(),
                "email", user.email(),
                "name", user.fullName(),
                "preferred_username", user.preferredUsername(),
                "given_name", user.firstName(),
                "family_name", user.lastName(),
                "picture", user.pictureUrl(),
                "email_verified", user.emailVerified()
        ));
    }

    private void handleKeycloakLogout(HttpExchange exchange) throws IOException {
        Map<String, String> form = form(exchange);
        KeycloakUser removed = REFRESH_TOKENS.remove(form.get("refresh_token"));
        if (removed == null) {
            send(exchange, 400, Map.of("error", "invalid_grant"));
            return;
        }
        sendNoContent(exchange, 204);
    }

    private void handleKeycloakAdminUsers(HttpExchange exchange) throws IOException {
        if (!("Bearer " + ADMIN_TOKEN).equals(exchange.getRequestHeaders().getFirst("Authorization"))) {
            send(exchange, 401, Map.of("message", "invalid_admin_token"));
            return;
        }

        String path = exchange.getRequestURI().getPath();
        if ("GET".equals(exchange.getRequestMethod())) {
            Map<String, String> query = query(exchange);
            KeycloakUser user = KEYCLOAK_USERS_BY_EMAIL.get(normalize(query.get("email")));
            send(exchange, 200, user == null ? List.of() : List.of(adminUserResponse(user)));
            return;
        }
        if ("POST".equals(exchange.getRequestMethod())) {
            createKeycloakUser(exchange);
            return;
        }
        if ("PUT".equals(exchange.getRequestMethod()) && path.contains("/reset-password")) {
            resetKeycloakPassword(exchange, path);
            return;
        }
        if ("PUT".equals(exchange.getRequestMethod())) {
            updateKeycloakUser(exchange, path);
            return;
        }
        send(exchange, 405, Map.of("message", "method_not_allowed"));
    }

    private void createKeycloakUser(HttpExchange exchange) throws IOException {
        Map<String, Object> request = OBJECT_MAPPER.readValue(exchange.getRequestBody(), new TypeReference<>() {
        });
        String email = String.valueOf(request.get("email"));
        if (KEYCLOAK_USERS_BY_EMAIL.containsKey(normalize(email))) {
            send(exchange, 409, Map.of("message", "user_exists"));
            return;
        }
        KeycloakUser user = keycloakUserFromPayload(UUID.randomUUID().toString(), request, null);
        storeKeycloakUser(user);
        send(exchange, 201, Map.of("id", user.id()));
    }

    private void updateKeycloakUser(HttpExchange exchange, String path) throws IOException {
        String id = userIdFromPath(path);
        KeycloakUser current = KEYCLOAK_USERS_BY_ID.get(id);
        if (current == null) {
            send(exchange, 404, Map.of("message", "user_not_found"));
            return;
        }
        Map<String, Object> request = OBJECT_MAPPER.readValue(exchange.getRequestBody(), new TypeReference<>() {
        });
        KeycloakUser updated = keycloakUserFromPayload(id, request, current.password());
        storeKeycloakUser(updated);
        sendNoContent(exchange, 204);
    }

    private void resetKeycloakPassword(HttpExchange exchange, String path) throws IOException {
        String id = userIdFromPath(path.replace("/reset-password", ""));
        KeycloakUser current = KEYCLOAK_USERS_BY_ID.get(id);
        if (current == null) {
            send(exchange, 404, Map.of("message", "user_not_found"));
            return;
        }
        Map<String, Object> request = OBJECT_MAPPER.readValue(exchange.getRequestBody(), new TypeReference<>() {
        });
        KeycloakUser updated = new KeycloakUser(current.id(), current.email(), current.fullName(),
                current.preferredUsername(), current.firstName(), current.lastName(), current.pictureUrl(),
                current.emailVerified(), String.valueOf(request.get("value")), current.tenantId(), current.userId(),
                current.roles(), current.providerSubject());
        storeKeycloakUser(updated);
        sendNoContent(exchange, 204);
    }

    private KeycloakUser oauthUser(String code) {
        if ("google-code".equals(code)) {
            return oauthUser("google-oauth@brasaller.test", "Google OAuth User", "google-oauth-subject");
        }
        return null;
    }

    private KeycloakUser oauthUser(String email, String fullName, String id) {
        KeycloakUser current = KEYCLOAK_USERS_BY_EMAIL.get(normalize(email));
        if (current != null) {
            return current;
        }
        KeycloakUser user = new KeycloakUser(id, email, fullName, email, fullName.split(" ", 2)[0],
                fullName.split(" ", 2)[1], "https://example.test/avatar.png", true, null,
                null, null, List.of(), id);
        storeKeycloakUser(user);
        return user;
    }

    private KeycloakUser keycloakUserFromPayload(String id, Map<String, Object> request, String existingPassword) {
        Map<String, Object> attributes = objectMap(request.get("attributes"));
        String email = String.valueOf(request.get("email"));
        String firstName = stringValue(request.get("firstName"), email);
        String lastName = stringValue(request.get("lastName"), "");
        String fullName = attr(attributes, "full_name", (firstName + " " + lastName).trim());
        String password = passwordFromCredentials(request.get("credentials"), existingPassword);
        return new KeycloakUser(
                id,
                email,
                fullName,
                stringValue(request.get("username"), email),
                firstName,
                lastName,
                "https://example.test/avatar.png",
                Boolean.parseBoolean(String.valueOf(request.getOrDefault("emailVerified", true))),
                password,
                attr(attributes, "tenant_id", null),
                attr(attributes, "user_id", null),
                attrList(attributes, "roles"),
                attr(attributes, "provider_subject", id)
        );
    }

    private Map<String, Object> userResponse(StoredIdentity identity) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", identity.userId());
        response.put("tenantId", identity.tenantId());
        response.put("email", identity.email());
        response.put("fullName", identity.fullName());
        response.put("preferredUsername", identity.preferredUsername());
        response.put("firstName", identity.firstName());
        response.put("lastName", identity.lastName());
        response.put("pictureUrl", identity.pictureUrl());
        response.put("emailVerified", identity.emailVerified());
        response.put("provider", identity.provider());
        response.put("providerSubject", identity.providerSubject());
        response.put("status", identity.status());
        response.put("roles", identity.roles());
        response.put("accountantTenantIds", List.of());
        return response;
    }

    private Map<String, Object> verificationResponse(StoredIdentity identity) {
        Map<String, Object> response = new HashMap<>();
        response.put("userId", identity.userId());
        response.put("tenantId", identity.tenantId());
        response.put("email", identity.email());
        response.put("fullName", identity.fullName());
        response.put("preferredUsername", identity.preferredUsername());
        response.put("firstName", identity.firstName());
        response.put("lastName", identity.lastName());
        response.put("pictureUrl", identity.pictureUrl());
        response.put("emailVerified", identity.emailVerified());
        response.put("provider", identity.provider());
        response.put("providerSubject", identity.providerSubject());
        response.put("roles", identity.roles());
        response.put("accountantTenantIds", List.of());
        return response;
    }

    private Map<String, Object> adminUserResponse(KeycloakUser user) {
        return Map.of(
                "id", user.id(),
                "username", user.email(),
                "email", user.email()
        );
    }

    private Map<String, Object> issueTokens(KeycloakUser user) {
        String accessToken = "kc-access-" + UUID.randomUUID();
        String refreshToken = "kc-refresh-" + UUID.randomUUID();
        ACCESS_TOKENS.put(accessToken, user);
        REFRESH_TOKENS.put(refreshToken, user);
        return tokenResponse(accessToken, refreshToken);
    }

    private Map<String, Object> tokenResponse(String accessToken, String refreshToken) {
        Map<String, Object> response = new HashMap<>();
        response.put("access_token", accessToken);
        response.put("id_token", "kc-id-" + UUID.randomUUID());
        response.put("token_type", "Bearer");
        response.put("expires_in", 300);
        if (refreshToken != null) {
            response.put("refresh_token", refreshToken);
        }
        return response;
    }

    private void storeKeycloakUser(KeycloakUser user) {
        KEYCLOAK_USERS_BY_ID.put(user.id(), user);
        KEYCLOAK_USERS_BY_EMAIL.put(normalize(user.email()), user);
    }

    private String userIdFromPath(String path) {
        String prefix = "/admin/realms/brasaller/users/";
        String value = path.substring(prefix.length());
        int slash = value.indexOf('/');
        return slash >= 0 ? value.substring(0, slash) : value;
    }

    private Map<String, String> form(HttpExchange exchange) throws IOException {
        String body = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        return parseUrlEncoded(body);
    }

    private Map<String, String> query(HttpExchange exchange) {
        return parseUrlEncoded(exchange.getRequestURI().getRawQuery());
    }

    private Map<String, String> parseUrlEncoded(String value) {
        Map<String, String> result = new HashMap<>();
        if (value == null || value.isBlank()) {
            return result;
        }
        for (String part : value.split("&")) {
            String[] pieces = part.split("=", 2);
            String name = URLDecoder.decode(pieces[0], StandardCharsets.UTF_8);
            String item = pieces.length > 1 ? URLDecoder.decode(pieces[1], StandardCharsets.UTF_8) : "";
            result.put(name, item);
        }
        return result;
    }

    private Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new HashMap<>();
            map.forEach((key, item) -> result.put(String.valueOf(key), item));
            return result;
        }
        return Map.of();
    }

    private String attr(Map<String, Object> attributes, String name, String fallback) {
        List<String> values = attrList(attributes, name);
        return values.isEmpty() ? fallback : values.getFirst();
    }

    private List<String> attrList(Map<String, Object> attributes, String name) {
        Object value = attributes.get(name);
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).toList();
        }
        if (value == null || value.toString().isBlank()) {
            return List.of();
        }
        return List.of(value.toString());
    }

    private String passwordFromCredentials(Object value, String fallback) {
        if (value instanceof List<?> credentials && !credentials.isEmpty()
                && credentials.getFirst() instanceof Map<?, ?> credential) {
            Object password = credential.get("value");
            return password == null ? fallback : password.toString();
        }
        return fallback;
    }

    private String stringValue(Object value, String fallback) {
        if (value == null || value.toString().isBlank() || "null".equals(value.toString())) {
            return fallback;
        }
        return value.toString();
    }

    private String normalize(String email) {
        return email == null ? "" : email.toLowerCase(Locale.ROOT);
    }

    private void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] response = OBJECT_MAPPER.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private void sendNoContent(HttpExchange exchange, int status) throws IOException {
        exchange.sendResponseHeaders(status, -1);
        exchange.close();
    }

    private record StoredIdentity(String tenantId, String userId, String email, String fullName, String preferredUsername,
                                  String firstName, String lastName, String pictureUrl, boolean emailVerified,
                                  String provider, String providerSubject, String password, List<String> roles,
                                  String status) {
        StoredIdentity withEmailVerified(boolean value) {
            return new StoredIdentity(tenantId, userId, email, fullName, preferredUsername, firstName, lastName,
                    pictureUrl, value, provider, providerSubject, password, roles, status);
        }

        StoredIdentity withStatus(String value) {
            return new StoredIdentity(tenantId, userId, email, fullName, preferredUsername, firstName, lastName,
                    pictureUrl, emailVerified, provider, providerSubject, password, roles, value);
        }

        StoredIdentity withPassword(String value) {
            return new StoredIdentity(tenantId, userId, email, fullName, preferredUsername, firstName, lastName,
                    pictureUrl, emailVerified, provider, providerSubject, value, roles, status);
        }
    }

    private record KeycloakUser(String id, String email, String fullName, String preferredUsername, String firstName,
                                String lastName, String pictureUrl, boolean emailVerified, String password,
                                String tenantId, String userId, List<String> roles, String providerSubject) {
    }
}
