package com.example.domain.model;

import java.time.Instant;

public record AgentAction(
        String id,
        String tenantId,
        String agentId,
        String executionId,
        String taskId,
        String actionType,
        String toolName,
        String inputJson,
        String outputJson,
        ActionStatus status,
        String error,
        long durationMs,
        Instant startedAt,
        Instant finishedAt,
        Instant createdAt
) {}
