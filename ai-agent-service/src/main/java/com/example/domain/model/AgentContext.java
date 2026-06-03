package com.example.domain.model;

import java.time.Instant;

public record AgentContext(
        String id,
        String tenantId,
        String agentId,
        ContextType contextType,
        String contextKey,
        String contextValue,
        Instant updatedAt
) {}
