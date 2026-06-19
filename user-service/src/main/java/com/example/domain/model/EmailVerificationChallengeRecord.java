package com.example.domain.model;

import java.time.Instant;

public record EmailVerificationChallengeRecord(
        String emailNormalized,
        String codeHash,
        String codeSalt,
        Instant expiresAt,
        int attemptsRemaining,
        Instant lastSentAt
) {
}
