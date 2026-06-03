package com.example.domain.model;

import java.time.Instant;

public record AgentGoal(
        String id,
        String tenantId,
        String agentId,
        String title,
        String description,
        String objective,
        int priority,
        GoalStatus status,
        Instant deadline,
        String result,
        Instant createdAt,
        Instant updatedAt
) {}
