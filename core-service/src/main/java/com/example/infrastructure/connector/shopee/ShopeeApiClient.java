package com.example.infrastructure.connector.shopee;

import com.example.application.exception.ConnectorRateLimitException;
import com.example.application.exception.ConnectorValidationException;
import org.eclipse.microprofile.faulttolerance.CircuitBreaker;
import java.time.temporal.ChronoUnit;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class ShopeeApiClient {
    private static final Logger LOG = Logger.getLogger(ShopeeApiClient.class);
    private static final String HMAC_SHA256 = "HmacSHA256";

    private HttpClient httpClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "shopee.api.base-url", defaultValue = "https://partner.shopeemobile.com")
    String baseUrl;

    @ConfigProperty(name = "shopee.api.connect-timeout-ms", defaultValue = "5000")
    long connectTimeoutMs;

    @ConfigProperty(name = "shopee.api.request-timeout-ms", defaultValue = "15000")
    long requestTimeoutMs;

    @ConfigProperty(name = "shopee.api.retry-max-attempts", defaultValue = "3")
    int retryMaxAttempts;

    @ConfigProperty(name = "shopee.api.retry-initial-delay-ms", defaultValue = "1000")
    long retryInitialDelayMs;

    @ConfigProperty(name = "shopee.api.retry-max-delay-ms", defaultValue = "30000")
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
    public JsonNode getPublic(String path, long partnerId, String partnerKey) {
        long timestamp = Instant.now().getEpochSecond();
        String sign = sign(partnerId + path + timestamp, partnerKey);
        String query = "partner_id=" + partnerId + "&timestamp=" + timestamp + "&sign=" + sign;
        return send(get(path + "?" + query));
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 60, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            skipOn = {ConnectorValidationException.class}
    )
    public JsonNode get(String path, long partnerId, String partnerKey, long shopId, String accessToken) {
        long timestamp = Instant.now().getEpochSecond();
        String sign = sign(partnerId + path + timestamp + accessToken + shopId, partnerKey);
        String query = "partner_id=" + partnerId + "&timestamp=" + timestamp + "&sign=" + sign
                + "&shop_id=" + shopId + "&access_token=" + encode(accessToken);
        return send(get(path + "?" + query));
    }

    @CircuitBreaker(
            requestVolumeThreshold = 10, failureRatio = 0.5,
            delay = 60, delayUnit = ChronoUnit.SECONDS,
            successThreshold = 3,
            skipOn = {ConnectorValidationException.class}
    )
    public JsonNode postPublic(String path, long partnerId, String partnerKey, JsonNode body) {
        long timestamp = Instant.now().getEpochSecond();
        String sign = sign(partnerId + path + timestamp, partnerKey);
        String query = "partner_id=" + partnerId + "&timestamp=" + timestamp + "&sign=" + sign;
        return send(post(path + "?" + query, body));
    }

    private HttpRequest get(String pathAndQuery) {
        return HttpRequest.newBuilder(URI.create(baseUrl + pathAndQuery))
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .header("Accept", "application/json")
                .GET()
                .build();
    }

    private HttpRequest post(String pathAndQuery, JsonNode body) {
        try {
            return HttpRequest.newBuilder(URI.create(baseUrl + pathAndQuery))
                    .timeout(Duration.ofMillis(requestTimeoutMs))
                    .header("Accept", "application/json")
                    .header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(body)))
                    .build();
        } catch (IOException e) {
            throw new ConnectorValidationException("shopee_request_serialization_error");
        }
    }

    private JsonNode send(HttpRequest request) {
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 429) {
                    if (attempt == retryMaxAttempts) {
                        throw new ConnectorRateLimitException("shopee_rate_limit_exhausted");
                    }
                    long delayMs = retryDelayMs(response, attempt);
                    LOG.warnf("Shopee rate limit hit (attempt %d/%d), retrying in %d ms", attempt, retryMaxAttempts, delayMs);
                    Thread.sleep(delayMs);
                    continue;
                }
                JsonNode body = parse(response.body());
                if (response.statusCode() >= 400) {
                    String message = text(body, "message");
                    if (message.isBlank()) message = text(body, "error");
                    throw new ConnectorValidationException("shopee_api_error: " + response.statusCode() + " " + message);
                }
                int errorCode = body.path("error").asInt(-1);
                if (errorCode > 0) {
                    throw new ConnectorValidationException("shopee_api_error: " + errorCode + " " + text(body, "message"));
                }
                return body;
            } catch (RuntimeException e) {
                throw e;
            } catch (IOException e) {
                throw new ConnectorValidationException("shopee_api_unavailable");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ConnectorValidationException("shopee_api_interrupted");
            }
        }
        throw new ConnectorRateLimitException("shopee_rate_limit_exhausted");
    }

    private long retryDelayMs(HttpResponse<?> response, int attempt) {
        String retryAfter = response.headers().firstValue("Retry-After").orElse(null);
        if (retryAfter != null) {
            try {
                return Math.min(Long.parseLong(retryAfter.trim()) * 1000L, retryMaxDelayMs);
            } catch (NumberFormatException ignored) {}
        }
        long exponential = retryInitialDelayMs * (1L << (attempt - 1));
        long jitter = (long) (ThreadLocalRandom.current().nextDouble() * retryInitialDelayMs);
        return Math.min(exponential + jitter, retryMaxDelayMs);
    }

    String sign(String data, String key) {
        try {
            Mac mac = Mac.getInstance(HMAC_SHA256);
            mac.init(new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), HMAC_SHA256));
            byte[] hash = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            throw new ConnectorValidationException("shopee_signing_error");
        }
    }

    ObjectNode newBody() {
        return objectMapper.createObjectNode();
    }

    private JsonNode parse(String body) throws IOException {
        if (body == null || body.isBlank()) return objectMapper.createObjectNode();
        return objectMapper.readTree(body);
    }

    private String text(JsonNode node, String field) {
        JsonNode value = node == null ? null : node.path(field);
        return value == null || value.isMissingNode() || value.isNull() ? "" : value.asText("");
    }

    private String encode(String value) {
        return URLEncoder.encode(value == null ? "" : value, StandardCharsets.UTF_8);
    }
}
