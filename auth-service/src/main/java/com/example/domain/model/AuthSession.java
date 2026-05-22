package com.example.domain.model;

import java.time.Instant;

public record AuthSession(String id, String tenantId, String userId, String tokenId, String refreshTokenHash,
                          Instant expiresAt) {
}
