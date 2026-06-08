package com.example.infrastructure.connector.shopee;

import java.time.Instant;

public record ShopeeConnectorToken(
        String tenantId,
        String shopId,
        String accessToken,
        String refreshToken,
        Instant expiresAt) {
}
