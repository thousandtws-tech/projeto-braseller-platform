package com.example.application.port.out;

import java.time.Instant;

public interface AuthNotificationGateway {
    void sendEmailVerificationCode(String email, String code, Instant expiresAt);

    void sendPasswordResetCode(String email, String code, Instant expiresAt);
}
