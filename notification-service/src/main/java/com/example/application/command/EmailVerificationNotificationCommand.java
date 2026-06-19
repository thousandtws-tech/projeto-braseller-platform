package com.example.application.command;

import java.time.Instant;

public record EmailVerificationNotificationCommand(
        String tenantId,
        String recipientEmail,
        String recipientName,
        String code,
        Instant expiresAt) {
}
