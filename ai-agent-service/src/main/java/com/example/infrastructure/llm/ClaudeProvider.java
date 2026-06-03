package com.example.infrastructure.llm;

import com.example.application.exception.LLMProviderException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Optional;

@ApplicationScoped
public class ClaudeProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(ClaudeProvider.class);
    private static final String CLAUDE_API_URL = "https://api.anthropic.com/v1/messages";
    private static final String ANTHROPIC_VERSION = "2023-06-01";

    @ConfigProperty(name = "ai.llm.claude.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "ai.llm.claude.model", defaultValue = "claude-haiku-4-5-20251001")
    String model;

    @ConfigProperty(name = "ai.llm.claude.timeout-seconds", defaultValue = "30")
    int timeoutSeconds;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String providerName() {
        return "claude";
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        if (!isAvailable()) {
            throw new LLMProviderException(503, "claude_not_configured");
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", request.maxTokens());

            if (request.systemRole() != null && !request.systemRole().isBlank()) {
                body.put("system", "You are an autonomous AI agent of type: " + request.systemRole());
            }

            ArrayNode messages = objectMapper.createArrayNode();
            ObjectNode user = objectMapper.createObjectNode();
            user.put("role", "user");
            user.put("content", request.prompt());
            messages.add(user);
            body.set("messages", messages);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(CLAUDE_API_URL))
                    .header("Content-Type", "application/json")
                    .header("x-api-key", apiKey.get())
                    .header("anthropic-version", ANTHROPIC_VERSION)
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new LLMProviderException(response.statusCode(),
                        "claude_error: " + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String content = json.path("content").get(0).path("text").asText();
            int inputTokens = json.path("usage").path("input_tokens").asInt(0);
            int outputTokens = json.path("usage").path("output_tokens").asInt(0);

            return new LLMResponse(content, model, inputTokens, outputTokens, 0.90);
        } catch (LLMProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.errorf(ex, "Claude request failed");
            throw new LLMProviderException(500, "claude_request_failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey.isPresent() && !apiKey.get().isBlank() && !"not-configured".equals(apiKey.get());
    }
}
