package com.example.infrastructure.tool;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

@ApplicationScoped
public class CallExternalWebhookTool implements AgentToolExecutor {

    private static final Logger LOG = Logger.getLogger(CallExternalWebhookTool.class);

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Override
    public String toolName() {
        return "call_webhook";
    }

    @Override
    public String description() {
        return "Call an external HTTP webhook with a JSON payload";
    }

    @Override
    public ToolResult execute(String tenantId, String inputJson) {
        try {
            JsonNode input = objectMapper.readTree(inputJson);
            String url = input.path("url").asText();
            String payload = input.path("payload").toString();

            if (url == null || url.isBlank()) {
                return ToolResult.failure("webhook_url_required");
            }

            LOG.infof("Executing call_webhook: tenantId=%s url=%s", tenantId, url);

            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("Content-Type", "application/json")
                    .header("X-Agent-Tenant-Id", tenantId)
                    .timeout(Duration.ofSeconds(15))
                    .POST(HttpRequest.BodyPublishers.ofString(payload))
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            return ToolResult.success(String.format(
                    "{\"status\":%d,\"body\":%s}",
                    response.statusCode(),
                    response.body().isEmpty() ? "null" : "\"" + response.body().replace("\"", "\\\"") + "\""
            ));
        } catch (Exception ex) {
            LOG.warnf("call_webhook failed: %s", ex.getMessage());
            return ToolResult.failure("webhook_call_failed: " + ex.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
