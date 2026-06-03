package com.example.domain.model;

import java.time.Instant;

public record AgentDecision(
        String id,
        String tenantId,
        String agentId,
        String executionId,
        String goalId,
        String contextJson,
        String reasoning,
        String decision,
        double confidence,
        String selectedTool,
        String toolInputJson,
        String outcome,
        Instant createdAt
) {}
