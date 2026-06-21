package com.example.domain.model;

import java.time.Instant;

public record AuthChallenge(
        String id,
        AuthChallengeType type,
        String email,
        String emailNormalized,
        String codeHash,
        Instant expiresAt,
        Instant usedAt,
        int attempts,
        boolean subjectExists,
        String requestIp,
        Instant createdAt) {
}
