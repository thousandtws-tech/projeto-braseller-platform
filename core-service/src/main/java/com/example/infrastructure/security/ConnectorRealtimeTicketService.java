package com.example.infrastructure.security;

import com.example.application.exception.InvalidTokenException;
import com.example.domain.model.TenantContext;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;

@ApplicationScoped
public class ConnectorRealtimeTicketService {
    private static final Base64.Encoder ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODER = Base64.getUrlDecoder();

    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "realtime.ticket.secret")
    String secret;

    @ConfigProperty(name = "realtime.ticket.ttl-seconds", defaultValue = "45")
    long ttlSeconds;

    public IssuedTicket issue(TenantContext context) {
        Instant expiresAt = Instant.now().plusSeconds(ttlSeconds);
        TicketClaims claims = new TicketClaims(
                context.tenantId(),
                context.userId(),
                expiresAt.getEpochSecond(),
                UUID.randomUUID().toString()
        );
        String payload = encode(claims);
        return new IssuedTicket(
                payload + "." + sign(payload),
                expiresAt,
                streamId(context.tenantId())
        );
    }

    public String requireTenant(String ticket) {
        try {
            String[] parts = ticket == null ? new String[0] : ticket.split("\\.");
            if (parts.length != 2) {
                throw new InvalidTokenException("malformed_realtime_ticket");
            }
            byte[] expected = DECODER.decode(sign(parts[0]));
            byte[] actual = DECODER.decode(parts[1]);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new InvalidTokenException("invalid_realtime_ticket");
            }
            TicketClaims claims = objectMapper.readValue(DECODER.decode(parts[0]), TicketClaims.class);
            if (Instant.ofEpochSecond(claims.expiresAt()).isBefore(Instant.now())) {
                throw new InvalidTokenException("expired_realtime_ticket");
            }
            if (claims.tenantId() == null || claims.tenantId().isBlank()) {
                throw new InvalidTokenException("invalid_realtime_ticket");
            }
            return claims.tenantId();
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException("invalid_realtime_ticket");
        }
    }

    private String streamId(String tenantId) {
        return sign("stream:" + tenantId).substring(0, 22);
    }

    private String encode(TicketClaims claims) {
        try {
            return ENCODER.encodeToString(objectMapper.writeValueAsBytes(claims));
        } catch (Exception exception) {
            throw new IllegalStateException("could_not_issue_realtime_ticket", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            return ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception exception) {
            throw new IllegalStateException("could_not_sign_realtime_ticket", exception);
        }
    }

    private record TicketClaims(String tenantId, String userId, long expiresAt, String nonce) {
    }

    public record IssuedTicket(String ticket, Instant expiresAt, String streamId) {
    }
}
