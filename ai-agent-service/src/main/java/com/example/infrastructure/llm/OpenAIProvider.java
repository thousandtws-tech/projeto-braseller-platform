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
public class OpenAIProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(OpenAIProvider.class);
    private static final String OPENAI_API_URL = "https://api.openai.com/v1/chat/completions";

    @ConfigProperty(name = "ai.llm.openai.api-key")
    Optional<String> apiKey;

    @ConfigProperty(name = "ai.llm.openai.model", defaultValue = "gpt-4o-mini")
    String model;

    @ConfigProperty(name = "ai.llm.openai.timeout-seconds", defaultValue = "30")
    int timeoutSeconds;

    @Inject
    ObjectMapper objectMapper;

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Override
    public String providerName() {
        return "openai";
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        if (!isAvailable()) {
            throw new LLMProviderException(503, "openai_not_configured");
        }
        try {
            ObjectNode body = objectMapper.createObjectNode();
            body.put("model", model);
            body.put("max_tokens", request.maxTokens());

            ArrayNode messages = objectMapper.createArrayNode();
            if (request.systemRole() != null && !request.systemRole().isBlank()) {
                ObjectNode system = objectMapper.createObjectNode();
                system.put("role", "system");
                system.put("content", "You are an autonomous AI agent of type: " + request.systemRole());
                messages.add(system);
            }
            ObjectNode user = objectMapper.createObjectNode();
            user.put("role", "user");
            user.put("content", request.prompt());
            messages.add(user);
            body.set("messages", messages);

            HttpRequest httpRequest = HttpRequest.newBuilder()
                    .uri(URI.create(OPENAI_API_URL))
                    .header("Content-Type", "application/json")
                    .header("Authorization", "Bearer " + apiKey.get())
                    .timeout(Duration.ofSeconds(timeoutSeconds))
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();

            HttpResponse<String> response = httpClient.send(httpRequest, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200) {
                throw new LLMProviderException(response.statusCode(),
                        "openai_error: " + response.statusCode());
            }

            JsonNode json = objectMapper.readTree(response.body());
            String content = json.path("choices").get(0).path("message").path("content").asText();
            int promptTokens = json.path("usage").path("prompt_tokens").asInt(0);
            int completionTokens = json.path("usage").path("completion_tokens").asInt(0);

            return new LLMResponse(content, model, promptTokens, completionTokens, 0.85);
        } catch (LLMProviderException ex) {
            throw ex;
        } catch (Exception ex) {
            LOG.errorf(ex, "OpenAI request failed");
            throw new LLMProviderException(500, "openai_request_failed: " + ex.getMessage(), ex);
        }
    }

    @Override
    public boolean isAvailable() {
        return apiKey.isPresent() && !apiKey.get().isBlank() && !"not-configured".equals(apiKey.get());
    }
}
