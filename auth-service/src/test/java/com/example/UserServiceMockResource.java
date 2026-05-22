package com.example;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

public class UserServiceMockResource implements QuarkusTestResourceLifecycleManager {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final String INTERNAL_TOKEN = "test-internal-token";
    private static final Map<String, StoredIdentity> IDENTITIES = new ConcurrentHashMap<>();

    private HttpServer server;

    @Override
    public Map<String, String> start() {
        try {
            IDENTITIES.clear();
            server = HttpServer.create(new InetSocketAddress(InetAddress.getLoopbackAddress(), 0), 0);
            server.createContext("/users/tenants/register", this::handleRegister);
            server.createContext("/users/internal/identity/verify-password", this::handleVerifyPassword);
            server.start();
            return Map.of(
                    "auth.user-service.url", "http://127.0.0.1:" + server.getAddress().getPort(),
                    "auth.user-service.internal-token", INTERNAL_TOKEN
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
        IDENTITIES.clear();
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
                request.get("password"),
                List.of("ADMIN", "VENDEDOR")
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
        if (identity == null || !identity.password().equals(request.get("password"))) {
            send(exchange, 401, Map.of("message", "invalid_credentials"));
            return;
        }

        send(exchange, 200, Map.of(
                "userId", identity.userId(),
                "tenantId", identity.tenantId(),
                "email", identity.email(),
                "fullName", identity.fullName(),
                "roles", identity.roles()
        ));
    }

    private Map<String, Object> userResponse(StoredIdentity identity) {
        return Map.of(
                "id", identity.userId(),
                "tenantId", identity.tenantId(),
                "email", identity.email(),
                "fullName", identity.fullName(),
                "status", "ACTIVE",
                "roles", identity.roles()
        );
    }

    private void send(HttpExchange exchange, int status, Object body) throws IOException {
        byte[] response = OBJECT_MAPPER.writeValueAsString(body).getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, response.length);
        exchange.getResponseBody().write(response);
        exchange.close();
    }

    private record StoredIdentity(String tenantId, String userId, String email, String fullName, String password, List<String> roles) {
    }
}
