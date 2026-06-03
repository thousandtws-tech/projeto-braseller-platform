package com.example.application.command;

public record CreateGoalCommand(
        String tenantId,
        String agentId,
        String title,
        String description,
        String objective,
        int priority,
        Long deadlineEpochSeconds
) {}
