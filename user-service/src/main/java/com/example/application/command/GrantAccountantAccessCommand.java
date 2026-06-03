package com.example.application.command;

public record GrantAccountantAccessCommand(
        String tenantId,
        String email,
        String firstName,
        String lastName,
        String fullName,
        String temporaryPassword,
        String grantedByUserId
) {}
