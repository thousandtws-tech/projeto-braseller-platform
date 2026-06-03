package com.example.domain.model;

import java.time.Instant;

public record AgentTool(
        String id,
        String name,
        String description,
        String toolType,
        boolean enabled,
        String configJson,
        Instant createdAt
) {}
