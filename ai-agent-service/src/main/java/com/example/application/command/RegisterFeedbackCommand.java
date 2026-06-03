package com.example.application.command;

public record RegisterFeedbackCommand(
        String tenantId,
        String agentId,
        String executionId,
        String feedbackType,
        int score,
        String comment,
        String metadataJson
) {}
