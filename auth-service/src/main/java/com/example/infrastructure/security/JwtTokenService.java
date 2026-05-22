package com.example.infrastructure.security;

import com.example.application.port.out.RefreshTokenHasher;
import com.example.application.port.out.TokenIssuer;
import com.example.domain.model.AuthIdentity;
import com.example.domain.model.IssuedTokens;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Instant;
import java.util.Base64;
import java.util.HashSet;
import java.util.UUID;

@ApplicationScoped
public class JwtTokenService implements TokenIssuer, RefreshTokenHasher {
    private static final SecureRandom RANDOM = new SecureRandom();

    @ConfigProperty(name = "auth.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "auth.jwt.audience")
    String audience;

    @ConfigProperty(name = "auth.jwt.secret")
    String secret;

    @ConfigProperty(name = "auth.jwt.access-token-ttl-seconds")
    long accessTokenTtlSeconds;

    @ConfigProperty(name = "auth.jwt.refresh-token-ttl-seconds")
    long refreshTokenTtlSeconds;

    @Override
    public IssuedTokens issue(AuthIdentity identity) {
        String tokenId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plusSeconds(accessTokenTtlSeconds);
        Instant refreshExpiresAt = now.plusSeconds(refreshTokenTtlSeconds);
        String accessToken = Jwt.issuer(issuer)
                .audience(audience)
                .subject(identity.userId())
                .upn(identity.email())
                .groups(new HashSet<>(identity.roles()))
                .issuedAt(now)
                .expiresAt(accessExpiresAt)
                .claim("jti", tokenId)
                .claim("tenant_id", identity.tenantId())
                .claim("user_id", identity.userId())
                .claim("email", identity.email())
                .claim("roles", identity.roles())
                .signWithSecret(secret);
        return new IssuedTokens(tokenId, accessToken, randomToken(), accessExpiresAt, refreshExpiresAt);
    }

    @Override
    public String hash(String refreshToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(refreshToken.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hash);
        } catch (Exception exception) {
            throw new IllegalStateException("Could not hash refresh token", exception);
        }
    }

    private String randomToken() {
        byte[] bytes = new byte[48];
        RANDOM.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
