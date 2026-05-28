package com.example.infrastructure.connector.mercadolivre;

import com.example.application.exception.ConnectorValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.util.Map;
import java.util.StringJoiner;

@ApplicationScoped
public class MercadoLivreApiClient {
    private static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(15);

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "mercadolivre.api.base-url", defaultValue = "https://api.mercadolibre.com")
    String baseUrl;

    public JsonNode exchangeCode(String clientId, String clientSecret, String redirectUri, String code) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("redirect_uri", redirectUri);
        form.put("code", code);
        return postForm("/oauth/token", form);
    }

    public JsonNode refreshToken(String clientId, String clientSecret, String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("refresh_token", refreshToken);
        return postForm("/oauth/token", form);
    }

    public JsonNode get(String path, String accessToken) {
        return get(path, accessToken, Map.of());
    }

    public JsonNode get(String path, String accessToken, Map<String, String> queryParameters) {
        HttpRequest request = HttpRequest.newBuilder(uri(path, queryParameters))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return send(request);
    }

    private JsonNode postForm(String path, Map<String, String> form) {
        HttpRequest request = HttpRequest.newBuilder(uri(path, Map.of()))
                .timeout(REQUEST_TIMEOUT)
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody(form)))
                .build();
        return send(request);
    }

    private JsonNode send(HttpRequest request) {
        try {
            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            JsonNode body = parse(response.body());
            if (response.statusCode() >= 400) {
                String message = text(body, "message");
                if (message.isBlank()) {
                    message = text(body, "error");
                }
                throw new ConnectorValidationException("mercado_livre_api_error: " + response.statusCode() + " " + message);
            }
            return body;
        } catch (IOException exception) {
            throw new ConnectorValidationException("mercado_livre_api_unavailable");
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new ConnectorValidationException("mercado_livre_api_interrupted");
        }
    }

    private JsonNode parse(String body) throws IOException {
        if (body == null || body.isBlank()) {
            return objectMapper.createObjectNode();
        }
        return objectMapper.readTree(body);
    }

    private URI uri(String path, Map<String, String> queryParameters) {
        String normalizedBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;
        String normalizedPath = path.startsWith("/") ? path : "/" + path;
        String query = queryString(queryParameters);
        return URI.create(normalizedBaseUrl + normalizedPath + (query.isBlank() ? "" : "?" + query));
    }

    private String queryString(Map<String, String> queryParameters) {
        if (queryParameters == null || queryParameters.isEmpty()) {
            return "";
        }
        StringJoiner joiner = new StringJoiner("&");
        queryParameters.forEach((name, value) -> {
            if (value != null && !value.isBlank()) {
                joiner.add(encode(name) + "=" + encode(value));
            }
        });
        return joiner.toString();
    }

    private String formBody(Map<String, String> form) {
        StringJoiner joiner = new StringJoiner("&");
        form.forEach((name, value) -> joiner.add(encode(name) + "=" + encode(value)));
        return joiner.toString();
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        return value == null || value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }
}
