package com.example.application.command;

public record ExecuteManualActionCommand(
        String tenantId,
        String agentId,
        String toolName,
        String inputJson
) {}
