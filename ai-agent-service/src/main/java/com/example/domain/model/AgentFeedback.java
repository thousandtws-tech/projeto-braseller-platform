package com.example.domain.model;

import java.time.Instant;

public record AgentFeedback(
        String id,
        String tenantId,
        String agentId,
        String executionId,
        FeedbackType feedbackType,
        int score,
        String comment,
        String metadataJson,
        Instant createdAt
) {}
