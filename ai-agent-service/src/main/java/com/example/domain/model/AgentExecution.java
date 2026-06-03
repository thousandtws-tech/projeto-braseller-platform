package com.example.domain.model;

import java.time.Instant;

public record AgentExecution(
        String id,
        String tenantId,
        String agentId,
        String goalId,
        String triggeredBy,
        ExecutionStatus status,
        int totalActions,
        int successActions,
        int failedActions,
        String summary,
        String error,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
) {}
