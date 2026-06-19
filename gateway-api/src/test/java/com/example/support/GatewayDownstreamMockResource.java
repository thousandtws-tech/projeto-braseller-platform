package com.example.support;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import io.quarkus.test.common.QuarkusTestResourceLifecycleManager;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public class GatewayDownstreamMockResource implements QuarkusTestResourceLifecycleManager {
    private HttpServer server;

    @Override
    public Map<String, String> start() {
        try {
            server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
            server.createContext("/", this::handle);
            server.start();
            String baseUrl = "http://127.0.0.1:" + server.getAddress().getPort();
            return Map.of(
                    "gateway.services.auth.url", baseUrl,
                    "gateway.services.user.url", baseUrl,
                    "gateway.services.core.url", baseUrl,
                    "gateway.services.billing.url", baseUrl,
                    "gateway.services.notification.url", baseUrl,
                    "gateway.services.reporting.url", baseUrl,
                    "quarkus.rest-client.gateway-downstream.url", baseUrl,
                    "quarkus.rest-client.core-realtime.url", baseUrl,
                    "quarkus.flyway.migrate-at-start", "false"
            );
        } catch (IOException exception) {
            throw new IllegalStateException("Could not start downstream mock server", exception);
        }
    }

    @Override
    public void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    private void handle(HttpExchange exchange) throws IOException {
        if (exchange.getRequestURI().getPath().equals("/core/connectors/events")) {
            boolean authorized = "Bearer test-token".equals(
                    exchange.getRequestHeaders().getFirst("Authorization"));
            boolean cursorForwarded = "cursor=41".equals(exchange.getRequestURI().getRawQuery());
            boolean lastEventForwarded = "41".equals(
                    exchange.getRequestHeaders().getFirst("Last-Event-ID"));
            if (!authorized || !cursorForwarded || !lastEventForwarded) {
                exchange.sendResponseHeaders(400, -1);
                exchange.close();
                return;
            }
            String payload = """
                    id: 42
                    event: connector.sync-job.processing.v1
                    data: {"sequence":42,"event_type":"connector.sync-job.processing.v1","aggregate_id":"job-42","payload":{"status":"PROCESSING"}}

                    : keepalive

                    """;
            exchange.getResponseHeaders().set("Content-Type", "text/event-stream");
            exchange.getResponseHeaders().set("Cache-Control", "no-cache");
            exchange.sendResponseHeaders(200, 0);
            exchange.getResponseBody().write(payload.getBytes(StandardCharsets.UTF_8));
            exchange.close();
            return;
        }

        if (exchange.getRequestURI().getPath().contains("/exports/")) {
            byte[] payload = "%PDF-1.4\nmock export\n%%EOF".getBytes(StandardCharsets.US_ASCII);
            exchange.getResponseHeaders().set("Content-Type", "application/pdf");
            exchange.getResponseHeaders().set("Content-Disposition", "attachment; filename=\"relatorio.pdf\"");
            exchange.getResponseHeaders().set("X-Report-Filename", "relatorio.pdf");
            exchange.sendResponseHeaders(200, payload.length);
            exchange.getResponseBody().write(payload);
            exchange.close();
            return;
        }

        String requestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
        String response = """
                {
                  "method": %s,
                  "path": %s,
                  "query": %s,
                  "authorization": %s,
                  "billing_webhook_token": %s,
                  "content_hmac": %s,
                  "body": %s
                }
                """.formatted(
                json(exchange.getRequestMethod()),
                json(exchange.getRequestURI().getPath()),
                json(exchange.getRequestURI().getRawQuery() == null ? "" : exchange.getRequestURI().getRawQuery()),
                json(exchange.getRequestHeaders().getFirst("Authorization")),
                json(exchange.getRequestHeaders().getFirst("X-Billing-Webhook-Token")),
                json(exchange.getRequestHeaders().getFirst("Content-Hmac")),
                json(requestBody)
        );

        byte[] payload = response.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().set("Content-Type", "application/json");
        exchange.getResponseHeaders().set("X-Downstream-Service", "mock");
        exchange.sendResponseHeaders(200, payload.length);
        exchange.getResponseBody().write(payload);
        exchange.close();
    }

    private static String json(String value) {
        if (value == null) {
            return "null";
        }
        return "\"" + value
                .replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\r", "\\r")
                .replace("\n", "\\n") + "\"";
    }
}
