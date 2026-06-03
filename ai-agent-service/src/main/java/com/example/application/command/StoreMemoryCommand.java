package com.example.application.command;

public record StoreMemoryCommand(
        String tenantId,
        String agentId,
        String memoryType,
        String memoryKey,
        String memoryValue,
        Integer ttlSeconds
) {}
