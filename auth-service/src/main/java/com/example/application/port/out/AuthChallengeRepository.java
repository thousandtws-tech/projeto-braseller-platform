package com.example.application.port.out;

import com.example.domain.model.AuthChallenge;
import com.example.domain.model.AuthChallengeType;

import java.time.Instant;
import java.util.Optional;

public interface AuthChallengeRepository {
    AuthChallenge create(String email, String normalizedEmail, AuthChallengeType type, String codeHash,
                         Instant expiresAt, String requestIp, boolean subjectExists);

    void invalidateOpenChallenges(String normalizedEmail, AuthChallengeType type);

    Optional<AuthChallenge> findLatestOpenChallenge(String normalizedEmail, AuthChallengeType type, Instant now);

    void incrementAttempts(String challengeId);

    void consume(String challengeId, Instant usedAt);

    long countRecentRequests(String normalizedEmail, String requestIp, AuthChallengeType type, Instant since);
}
