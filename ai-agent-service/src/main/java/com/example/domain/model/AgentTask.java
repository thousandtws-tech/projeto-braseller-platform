package com.example.domain.model;

import java.time.Instant;

public record AgentTask(
        String id,
        String tenantId,
        String agentId,
        String goalId,
        String executionId,
        String title,
        String taskType,
        ExecutionStatus status,
        String inputJson,
        String outputJson,
        String toolName,
        int retryCount,
        String error,
        Instant createdAt,
        Instant updatedAt
) {}
