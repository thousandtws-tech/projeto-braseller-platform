package com.example.infrastructure.security;

import com.example.application.port.out.TokenIssuer;
import com.example.domain.model.AuthIdentity;
import com.example.domain.model.IssuedTokens;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Instant;
import java.util.HashSet;
import java.util.UUID;

@ApplicationScoped
public class JwtTokenService implements TokenIssuer {
    @ConfigProperty(name = "auth.jwt.issuer")
    String issuer;

    @ConfigProperty(name = "auth.jwt.audience")
    String audience;

    @ConfigProperty(name = "auth.jwt.secret")
    String secret;

    @ConfigProperty(name = "auth.jwt.access-token-ttl-seconds")
    long accessTokenTtlSeconds;

    @Override
    public IssuedTokens issue(AuthIdentity identity) {
        String tokenId = UUID.randomUUID().toString();
        Instant now = Instant.now();
        Instant accessExpiresAt = now.plusSeconds(accessTokenTtlSeconds);
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
        return new IssuedTokens(accessToken, accessExpiresAt);
    }
}
