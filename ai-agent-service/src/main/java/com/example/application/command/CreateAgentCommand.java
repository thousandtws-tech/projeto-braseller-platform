package com.example.application.command;

public record CreateAgentCommand(
        String tenantId,
        String name,
        String description,
        String agentType,
        String capabilities
) {}
