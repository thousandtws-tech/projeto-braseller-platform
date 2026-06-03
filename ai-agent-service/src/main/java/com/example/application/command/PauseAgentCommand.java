package com.example.application.command;

public record PauseAgentCommand(String tenantId, String agentId, String reason) {}
