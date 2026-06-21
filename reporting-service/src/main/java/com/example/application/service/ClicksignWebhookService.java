package com.example.application.service;

import com.example.application.exception.ForbiddenException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.ClicksignWebhookEventRepository;
import com.example.domain.model.ClicksignWebhookEvent;
import com.example.infrastructure.client.ApiIntegrationEventRequest;
import com.example.infrastructure.client.CoreIntegrationsRestClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.format.DateTimeParseException;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

@ApplicationScoped
public class ClicksignWebhookService {
    private static final Logger LOG = Logger.getLogger(ClicksignWebhookService.class);
    private static final String INTEGRATION_NAME = "clicksign";
    private static final String SYSTEM_TENANT_ID = "__system__";

    private final ObjectMapper objectMapper;
    private final ClicksignWebhookEventRepository repository;

    @ConfigProperty(name = "reporting.clicksign.webhook-secret")
    String webhookSecret;

    @Inject
    @RestClient
    CoreIntegrationsRestClient coreIntegrationsRestClient;

    @ConfigProperty(name = "reporting.internal-token")
    String internalToken;

    @Inject
    public ClicksignWebhookService(ObjectMapper objectMapper, ClicksignWebhookEventRepository repository) {
        this.objectMapper = objectMapper;
        this.repository = repository;
    }

    public ClicksignWebhookEvent receive(String payload, String contentHmac) {
        String rawPayload = requirePayload(payload);
        verifySignature(rawPayload, contentHmac);

        JsonNode root = readPayload(rawPayload);
        String eventName = text(root, "/event/name");
        if (eventName == null) {
            throw new ValidationException("clicksign_event_name_required");
        }

        Instant now = Instant.now();
        ClicksignWebhookEvent event = new ClicksignWebhookEvent(
                UUID.randomUUID().toString(),
                eventName,
                firstText(root, "/event/data/account/key", "/account/key", "/account_key"),
                firstText(root, "/event/data/envelope/id", "/event/data/envelope/key", "/envelope/id", "/envelope/key", "/envelope_id"),
                firstText(root, "/document/key", "/document/id", "/document/data/id", "/document_key"),
                parseInstant(text(root, "/event/occurred_at")),
                statusFor(eventName),
                messageFor(eventName),
                now,
                now
        );
        return repository.save(event, rawPayload, contentHmac == null ? null : contentHmac.trim());
    }

    private String requirePayload(String payload) {
        if (payload == null || payload.isBlank()) {
            throw new ValidationException("clicksign_payload_required");
        }
        return payload;
    }

    private JsonNode readPayload(String payload) {
        try {
            return objectMapper.readTree(payload);
        } catch (Exception exception) {
            throw new ValidationException("clicksign_payload_invalid");
        }
    }

    private void verifySignature(String payload, String contentHmac) {
        if (isNotConfigured(webhookSecret)) {
            return;
        }
        if (contentHmac == null || contentHmac.isBlank()) {
            recordAuthFailure("clicksign_hmac_required");
            throw new ForbiddenException("clicksign_hmac_required");
        }
        String expected = "sha256=" + hmacSha256(payload, webhookSecret.trim());
        if (!constantTimeEquals(expected, contentHmac.trim())) {
            recordAuthFailure("clicksign_hmac_invalid");
            throw new ForbiddenException("clicksign_hmac_invalid");
        }
    }

    private void recordAuthFailure(String errorMessage) {
        try (jakarta.ws.rs.core.Response response = coreIntegrationsRestClient.recordEvent(internalToken, new ApiIntegrationEventRequest(
                SYSTEM_TENANT_ID,
                INTEGRATION_NAME,
                "/webhooks/clicksign",
                "verify_signature",
                Instant.now(),
                null,
                null,
                "FAILURE",
                "AUTH_FAILURE",
                "CRITICAL",
                "Webhook do Clicksign rejeitado por assinatura HMAC invalida ou ausente",
                "manual_review_required",
                errorMessage
        ))) {
            response.bufferEntity();
        } catch (RuntimeException exception) {
            LOG.warnf(exception, "Failed to record api integration event for %s", INTEGRATION_NAME);
        }
    }

    private String hmacSha256(String payload, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return HexFormat.of().formatHex(mac.doFinal(payload.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("clicksign_hmac_failed", exception);
        }
    }

    private boolean constantTimeEquals(String expected, String actual) {
        return MessageDigest.isEqual(
                expected.getBytes(StandardCharsets.UTF_8),
                actual.getBytes(StandardCharsets.UTF_8)
        );
    }

    private String statusFor(String eventName) {
        String normalized = normalize(eventName);
        return switch (normalized) {
            case "document_closed", "close" -> "DOCUMENT_CLOSED";
            case "sign" -> "SIGNED";
            case "refusal" -> "REFUSED";
            case "cancel" -> "CANCELLED";
            default -> "RECEIVED";
        };
    }

    private String messageFor(String eventName) {
        String normalized = normalize(eventName);
        return switch (normalized) {
            case "document_closed", "close" -> "Documento Clicksign finalizado; pronto para conciliacao do fechamento contabil.";
            case "sign" -> "Assinatura Clicksign registrada.";
            case "refusal" -> "Assinatura Clicksign recusada.";
            case "cancel" -> "Documento ou envelope Clicksign cancelado.";
            default -> "Evento Clicksign recebido para auditoria.";
        };
    }

    private String normalize(String eventName) {
        return eventName == null ? "" : eventName.trim().toLowerCase(Locale.ROOT);
    }

    private Instant parseInstant(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return OffsetDateTime.parse(value.trim()).toInstant();
        } catch (DateTimeParseException exception) {
            try {
                return Instant.parse(value.trim());
            } catch (DateTimeParseException ignored) {
                return null;
            }
        }
    }

    private String firstText(JsonNode root, String... pointers) {
        for (String pointer : pointers) {
            String value = text(root, pointer);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private String text(JsonNode root, String pointer) {
        JsonNode node = root.at(pointer);
        if (node.isMissingNode() || node.isNull()) {
            return null;
        }
        String value = node.asText(null);
        return value == null || value.isBlank() ? null : value.trim();
    }

    private boolean isNotConfigured(String value) {
        return value == null || value.isBlank() || "not-configured".equalsIgnoreCase(value.trim());
    }
}
