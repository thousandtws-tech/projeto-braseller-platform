package com.example.domain.model;

import java.time.Instant;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "EmailVerificationChallenge", description = "Estado atual do envio do codigo de verificacao de e-mail.")
public record EmailVerificationChallenge(String email, Instant expiresAt, Instant lastSentAt) {
}
