package com.example.infrastructure.security;

import com.example.application.exception.InvalidTokenException;
import com.example.application.port.out.AccessTokenVerifier;
import com.example.domain.model.TenantContext;
import com.fasterxml.jackson.core.type.TypeReference;
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
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class Hs256JwtContextVerifier implements AccessTokenVerifier {
    @Inject
    ObjectMapper objectMapper;

    @ConfigProperty(name = "user.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "user.jwt.audience")
    String audience;

    @ConfigProperty(name = "user.jwt.secret")
    String secret;

    @Override
    public TenantContext verify(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("missing_bearer_token");
        }

        String token = authorizationHeader.substring("Bearer ".length()).trim();
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("malformed_token");
        }

        verifySignature(parts);
        Map<String, Object> claims = parseClaims(parts[1]);
        verifyRegisteredClaims(claims);

        String tenantId = stringClaim(claims, "tenant_id");
        String userId = stringClaim(claims, "user_id");
        String email = stringClaim(claims, "email");
        List<String> roles = listClaim(claims, "groups");
        if (roles.isEmpty()) {
            roles = listClaim(claims, "roles");
        }
        boolean readOnly = roles.contains("CONTADOR")
                && !roles.contains("ADMIN")
                && !roles.contains("BPO_ADMIN");
        return new TenantContext(tenantId, userId, email, roles, readOnly);
    }

    private void verifySignature(String[] parts) {
        try {
            String signingInput = parts[0] + "." + parts[1];
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
            byte[] expected = mac.doFinal(signingInput.getBytes(StandardCharsets.UTF_8));
            byte[] actual = Base64.getUrlDecoder().decode(parts[2]);
            if (!MessageDigest.isEqual(expected, actual)) {
                throw new InvalidTokenException("invalid_signature");
            }
        } catch (InvalidTokenException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new InvalidTokenException("signature_verification_failed");
        }
    }

    private Map<String, Object> parseClaims(String encodedPayload) {
        try {
            byte[] payload = Base64.getUrlDecoder().decode(encodedPayload);
            return objectMapper.readValue(payload, new TypeReference<>() {
            });
        } catch (Exception exception) {
            throw new InvalidTokenException("invalid_payload");
        }
    }

    private void verifyRegisteredClaims(Map<String, Object> claims) {
        if (!issuer.equals(claims.get("iss"))) {
            throw new InvalidTokenException("invalid_issuer");
        }
        if (!hasAudience(claims.get("aud"))) {
            throw new InvalidTokenException("invalid_audience");
        }
        Object exp = claims.get("exp");
        if (!(exp instanceof Number number) || Instant.ofEpochSecond(number.longValue()).isBefore(Instant.now())) {
            throw new InvalidTokenException("expired_token");
        }
    }

    private boolean hasAudience(Object claim) {
        if (claim instanceof String value) {
            return audience.equals(value);
        }
        if (claim instanceof List<?> values) {
            return values.contains(audience);
        }
        return false;
    }

    private String stringClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value == null || value.toString().isBlank()) {
            throw new InvalidTokenException("missing_" + name);
        }
        return value.toString();
    }

    private List<String> listClaim(Map<String, Object> claims, String name) {
        Object value = claims.get(name);
        if (value instanceof List<?> values) {
            return values.stream().map(Object::toString).sorted().toList();
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            return List.of(stringValue);
        }
        return List.of();
    }
}
