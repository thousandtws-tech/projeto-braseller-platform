package com.example.infrastructure.security;

import com.example.application.exception.InvalidTokenException;
import com.example.application.port.out.AccessTokenVerifier;
import com.example.domain.model.TenantContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;

@ApplicationScoped
public class Hs256JwtContextVerifier implements AccessTokenVerifier {

    @ConfigProperty(name = "ai.jwt.secret")
    String jwtSecret;

    @ConfigProperty(name = "ai.jwt.issuer", defaultValue = "brasaller-auth")
    String expectedIssuer;

    @Inject
    ObjectMapper objectMapper;

    @Override
    public TenantContext verify(String authorizationHeader) {
        if (authorizationHeader == null || !authorizationHeader.startsWith("Bearer ")) {
            throw new InvalidTokenException("missing_authorization_header");
        }
        String token = authorizationHeader.substring(7).trim();
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new InvalidTokenException("invalid_token_format");
        }
        try {
            verifySignature(parts[0], parts[1], parts[2]);
            String payloadJson = new String(Base64.getUrlDecoder().decode(padBase64(parts[1])), StandardCharsets.UTF_8);
            JsonNode payload = objectMapper.readTree(payloadJson);

            long exp = payload.path("exp").asLong(0);
            if (exp > 0 && Instant.ofEpochSecond(exp).isBefore(Instant.now())) {
                throw new InvalidTokenException("token_expired");
            }
            String issuer = payload.path("iss").asText();
            if (!expectedIssuer.equals(issuer)) {
                throw new InvalidTokenException("invalid_token_issuer");
            }
            String tenantId = payload.path("tenant_id").asText();
            String userId = payload.path("user_id").asText();
            String email = payload.path("email").asText();

            if (tenantId == null || tenantId.isBlank()) {
                throw new InvalidTokenException("missing_tenant_id_claim");
            }
            List<String> roles = new ArrayList<>();
            JsonNode rolesNode = payload.path("roles");
            if (rolesNode.isArray()) {
                rolesNode.forEach(r -> roles.add(r.asText()));
            }
            return new TenantContext(tenantId, userId, email, roles);
        } catch (InvalidTokenException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new InvalidTokenException("token_verification_failed: " + ex.getMessage());
        }
    }

    private void verifySignature(String header, String payload, String signature) throws Exception {
        String data = header + "." + payload;
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(jwtSecret.getBytes(StandardCharsets.UTF_8), "HmacSHA256"));
        byte[] expected = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
        byte[] actual = Base64.getUrlDecoder().decode(padBase64(signature));
        if (!java.security.MessageDigest.isEqual(expected, actual)) {
            throw new InvalidTokenException("invalid_token_signature");
        }
    }

    private String padBase64(String input) {
        int remainder = input.length() % 4;
        if (remainder == 0) return input;
        return input + "=".repeat(4 - remainder);
    }
}
