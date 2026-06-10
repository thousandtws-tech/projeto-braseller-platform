package com.example.infrastructure.connector.mercadolivre;

import com.example.application.exception.ConnectorRateLimitException;
import com.example.application.exception.ConnectorValidationException;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import java.time.temporal.ChronoUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

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
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class MercadoLivreApiClient {
    private static final Logger LOG = Logger.getLogger(MercadoLivreApiClient.class);

    private HttpClient httpClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "mercadolivre.api.base-url", defaultValue = "https://api.mercadolibre.com")
    String baseUrl;

    @ConfigProperty(name = "mercadolivre.api.connect-timeout-ms", defaultValue = "5000")
    long connectTimeoutMs;

    @ConfigProperty(name = "mercadolivre.api.request-timeout-ms", defaultValue = "15000")
    long requestTimeoutMs;

    @ConfigProperty(name = "mercadolivre.api.retry-max-attempts", defaultValue = "3")
    int retryMaxAttempts;

    @ConfigProperty(name = "mercadolivre.api.retry-initial-delay-ms", defaultValue = "1000")
    long retryInitialDelayMs;

    @ConfigProperty(name = "mercadolivre.api.retry-max-delay-ms", defaultValue = "30000")
    long retryMaxDelayMs;

    @PostConstruct
    void initHttpClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 60, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            skipOn = {ConnectorValidationException.class}
    )
    public JsonNode exchangeCode(String clientId, String clientSecret, String redirectUri, String code) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "authorization_code");
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("redirect_uri", redirectUri);
        form.put("code", code);
        return postForm("/oauth/token", form);
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 60, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            skipOn = {ConnectorValidationException.class}
    )
    public JsonNode refreshToken(String clientId, String clientSecret, String refreshToken) {
        Map<String, String> form = new LinkedHashMap<>();
        form.put("grant_type", "refresh_token");
        form.put("client_id", clientId);
        form.put("client_secret", clientSecret);
        form.put("refresh_token", refreshToken);
        return postForm("/oauth/token", form);
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 60, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            skipOn = {ConnectorValidationException.class}
    )
    public JsonNode get(String path, String accessToken) {
        return get(path, accessToken, Map.of());
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 60, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            skipOn = {ConnectorValidationException.class}
    )
    public JsonNode get(String path, String accessToken, Map<String, String> queryParameters) {
        HttpRequest request = HttpRequest.newBuilder(uri(path, queryParameters))
                .timeout(requestTimeout())
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();
        return send(request);
    }

    private JsonNode postForm(String path, Map<String, String> form) {
        HttpRequest request = HttpRequest.newBuilder(uri(path, Map.of()))
                .timeout(requestTimeout())
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody(form)))
                .build();
        return send(request);
    }

    private JsonNode send(HttpRequest request) {
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

                if (response.statusCode() == 429) {
                    if (attempt == retryMaxAttempts) {
                        throw new ConnectorRateLimitException("mercado_livre_rate_limit_exhausted");
                    }
                    long delayMs = retryDelayMs(response, attempt);
                    LOG.warnf("Mercado Livre rate limit hit (attempt %d/%d), retrying in %d ms", attempt, retryMaxAttempts, delayMs);
                    Thread.sleep(delayMs);
                    continue;
                }

                JsonNode body = parse(response.body());
                if (response.statusCode() >= 400) {
                    String message = text(body, "message");
                    if (message.isBlank()) {
                        message = text(body, "error");
                    }
                    throw new ConnectorValidationException("mercado_livre_api_error: " + response.statusCode() + " " + message);
                }
                return body;

            } catch (RuntimeException e) {
                throw e;
            } catch (IOException e) {
                throw new ConnectorValidationException("mercado_livre_api_unavailable");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ConnectorValidationException("mercado_livre_api_interrupted");
            }
        }
        throw new ConnectorRateLimitException("mercado_livre_rate_limit_exhausted");
    }

    private long retryDelayMs(HttpResponse<?> response, int attempt) {
        // Respect Retry-After header sent by ML API (value in seconds)
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
        if (retryAfter != null) {
            try {
                long serverDelayMs = Long.parseLong(retryAfter.trim()) * 1000L;
                return Math.min(serverDelayMs, retryMaxDelayMs);
            } catch (NumberFormatException ignored) {}
        }
        // Exponential backoff with jitter: 1s, 2s, 4s, ... capped at retryMaxDelayMs
        long exponential = retryInitialDelayMs * (1L << (attempt - 1));
        long jitter = (long) (ThreadLocalRandom.current().nextDouble() * retryInitialDelayMs);
        return Math.min(exponential + jitter, retryMaxDelayMs);
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

    private Duration requestTimeout() {
        return Duration.ofMillis(requestTimeoutMs);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        return value == null || value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }
}
