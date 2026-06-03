package com.example.domain.model;

import java.time.Instant;

public record AgentMemory(
        String id,
        String tenantId,
        String agentId,
        MemoryType memoryType,
        String memoryKey,
        String memoryValue,
        Integer ttlSeconds,
        Instant createdAt,
        Instant expiresAt
) {}
