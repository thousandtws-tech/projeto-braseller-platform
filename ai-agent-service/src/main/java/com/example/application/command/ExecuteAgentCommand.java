package com.example.application.command;

public record ExecuteAgentCommand(
        String tenantId,
        String agentId,
        String goalId,
        String triggeredBy
) {}
