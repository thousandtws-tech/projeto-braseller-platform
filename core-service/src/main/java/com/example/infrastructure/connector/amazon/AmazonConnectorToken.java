package com.example.infrastructure.connector.amazon;

import java.time.Instant;

public record AmazonConnectorToken(
        String tenantId,
        String sellerId,
        String accessToken,
        String refreshToken,
        Instant expiresAt) {
}
