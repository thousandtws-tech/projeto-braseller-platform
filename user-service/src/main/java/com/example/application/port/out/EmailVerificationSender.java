package com.example.application.port.out;

import java.time.Instant;

public interface EmailVerificationSender {
    void send(String tenantId, String recipientEmail, String recipientName, String code, Instant expiresAt);
}
