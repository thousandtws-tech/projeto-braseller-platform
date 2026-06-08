package com.example.infrastructure.connector.amazon;

import com.example.application.exception.ConnectorRateLimitException;
import com.example.application.exception.ConnectorValidationException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import java.security.MessageDigest;
import java.time.Duration;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.StringJoiner;
import java.util.concurrent.ThreadLocalRandom;

@ApplicationScoped
public class AmazonApiClient {
    private static final Logger LOG = Logger.getLogger(AmazonApiClient.class);
    private static final String LWA_URL = "https://api.amazon.com/auth/o2/token";
    private static final String SERVICE = "execute-api";
    private static final String ALGORITHM = "AWS4-HMAC-SHA256";
    private static final DateTimeFormatter DATE_TIME_FMT = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyyMMdd");

    private HttpClient httpClient;

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "amazon.api.base-url", defaultValue = "https://sellingpartnerapi-na.amazon.com")
    String baseUrl;

    @ConfigProperty(name = "amazon.api.region", defaultValue = "us-east-1")
    String region;

    @ConfigProperty(name = "amazon.api.connect-timeout-ms", defaultValue = "5000")
    long connectTimeoutMs;

    @ConfigProperty(name = "amazon.api.request-timeout-ms", defaultValue = "15000")
    long requestTimeoutMs;

    @ConfigProperty(name = "amazon.api.retry-max-attempts", defaultValue = "3")
    int retryMaxAttempts;

    @ConfigProperty(name = "amazon.api.retry-initial-delay-ms", defaultValue = "1000")
    long retryInitialDelayMs;

    @ConfigProperty(name = "amazon.api.retry-max-delay-ms", defaultValue = "30000")
    long retryMaxDelayMs;

    @PostConstruct
    void initHttpClient() {
        httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .build();
    }

    public JsonNode exchangeCode(String clientId, String clientSecret, String code) {
        String body = "grant_type=authorization_code&code=" + encode(code)
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret);
        return postLwa(body);
    }

    public JsonNode refreshToken(String clientId, String clientSecret, String refreshToken) {
        String body = "grant_type=refresh_token&refresh_token=" + encode(refreshToken)
                + "&client_id=" + encode(clientId)
                + "&client_secret=" + encode(clientSecret);
        return postLwa(body);
    }

    public JsonNode get(String path, String accessToken, String awsAccessKey, String awsSecretKey,
                        Map<String, String> queryParams) {
        ZonedDateTime now = ZonedDateTime.now(ZoneOffset.UTC);
        String dateTime = now.format(DATE_TIME_FMT);
        String date = now.format(DATE_FMT);
        String queryString = queryString(queryParams);
        String url = baseUrl + path + (queryString.isBlank() ? "" : "?" + queryString);

        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("host", URI.create(baseUrl).getHost());
        headers.put("x-amz-access-token", accessToken);
        headers.put("x-amz-date", dateTime);

        String authorization = sigV4Authorization("GET", path, queryString, headers, "", date, dateTime, awsAccessKey, awsSecretKey);
        headers.put("Authorization", authorization);

        HttpRequest request = HttpRequest.newBuilder(URI.create(url))
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .header("Accept", "application/json")
                .header("x-amz-access-token", accessToken)
                .header("x-amz-date", dateTime)
                .header("Authorization", authorization)
                .GET()
                .build();
        return send(request);
    }

    private JsonNode postLwa(String formBody) {
        HttpRequest request = HttpRequest.newBuilder(URI.create(LWA_URL))
                .timeout(Duration.ofMillis(requestTimeoutMs))
                .header("Accept", "application/json")
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(formBody))
                .build();
        return send(request);
    }

    private String sigV4Authorization(String method, String path, String queryString,
                                      Map<String, String> headers, String payload,
                                      String date, String dateTime,
                                      String accessKey, String secretKey) {
        String payloadHash = sha256Hex(payload);
        String canonicalHeaders = canonicalHeaders(headers);
        String signedHeaders = String.join(";", headers.keySet());
        String canonicalRequest = method + "\n" + path + "\n" + queryString + "\n"
                + canonicalHeaders + "\n" + signedHeaders + "\n" + payloadHash;
        String credentialScope = date + "/" + region + "/" + SERVICE + "/aws4_request";
        String stringToSign = ALGORITHM + "\n" + dateTime + "\n" + credentialScope + "\n" + sha256Hex(canonicalRequest);
        byte[] signingKey = signingKey(secretKey, date);
        String signature = hmacHex(signingKey, stringToSign);
        return ALGORITHM + " Credential=" + accessKey + "/" + credentialScope
                + ", SignedHeaders=" + signedHeaders + ", Signature=" + signature;
    }

    private byte[] signingKey(String secretKey, String date) {
        byte[] kDate = hmac(("AWS4" + secretKey).getBytes(StandardCharsets.UTF_8), date);
        byte[] kRegion = hmac(kDate, region);
        byte[] kService = hmac(kRegion, SERVICE);
        return hmac(kService, "aws4_request");
    }

    private String canonicalHeaders(Map<String, String> headers) {
        StringBuilder sb = new StringBuilder();
        headers.forEach((k, v) -> sb.append(k.toLowerCase()).append(":").append(v.strip()).append("\n"));
        return sb.toString();
    }

    private String sha256Hex(String data) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hash = md.digest(data.getBytes(StandardCharsets.UTF_8));
            return hexEncode(hash);
        } catch (Exception e) {
            throw new ConnectorValidationException("amazon_sha256_error");
        }
    }

    private byte[] hmac(byte[] key, String data) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(key, "HmacSHA256"));
            return mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new ConnectorValidationException("amazon_hmac_error");
        }
    }

    private String hmacHex(byte[] key, String data) {
        return hexEncode(hmac(key, data));
    }

    private String hexEncode(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    private String queryString(Map<String, String> params) {
        if (params == null || params.isEmpty()) return "";
        StringJoiner joiner = new StringJoiner("&");
        new java.util.TreeMap<>(params).forEach((k, v) -> joiner.add(encode(k) + "=" + encode(v)));
        return joiner.toString();
    }

    private JsonNode send(HttpRequest request) {
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
                if (response.statusCode() == 429) {
                    if (attempt == retryMaxAttempts) throw new ConnectorRateLimitException("amazon_rate_limit_exhausted");
                    long delayMs = retryDelayMs(response, attempt);
                    LOG.warnf("Amazon rate limit hit (attempt %d/%d), retrying in %d ms", attempt, retryMaxAttempts, delayMs);
                    Thread.sleep(delayMs);
                    continue;
                }
                JsonNode body = parse(response.body());
                if (response.statusCode() >= 400) {
                    String message = errorMessage(body);
                    throw new ConnectorValidationException("amazon_api_error: " + response.statusCode() + " " + message);
                }
                return body;
            } catch (RuntimeException e) {
                throw e;
            } catch (IOException e) {
                throw new ConnectorValidationException("amazon_api_unavailable");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new ConnectorValidationException("amazon_api_interrupted");
            }
        }
        throw new ConnectorRateLimitException("amazon_rate_limit_exhausted");
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

    private String errorMessage(JsonNode body) {
        if (body.has("errors") && body.get("errors").isArray() && !body.get("errors").isEmpty()) {
            JsonNode first = body.get("errors").get(0);
            return text(first, "message");
        }
        String msg = text(body, "error_description");
        return msg.isBlank() ? text(body, "error") : msg;
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
