package com.example.domain.model;

import java.time.Instant;

public record Agent(
        String id,
        String tenantId,
        String name,
        String description,
        String agentType,
        String capabilities,
        AgentStatus status,
        Instant createdAt,
        Instant updatedAt
) {}
