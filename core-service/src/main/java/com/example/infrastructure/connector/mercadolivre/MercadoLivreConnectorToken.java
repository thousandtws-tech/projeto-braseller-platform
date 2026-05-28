package com.example.infrastructure.connector.mercadolivre;

import java.time.Instant;

public record MercadoLivreConnectorToken(
        String tenantId,
        String sellerId,
        String accessToken,
        String refreshToken,
        Instant expiresAt) {
}
