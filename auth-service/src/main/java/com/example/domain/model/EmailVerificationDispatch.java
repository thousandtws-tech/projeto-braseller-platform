package com.example.domain.model;

import java.time.Instant;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "EmailVerificationDispatch", description = "Resposta do envio ou reenvio do codigo de verificacao.")
public record EmailVerificationDispatch(String email, Instant expiresAt, Instant lastSentAt) {
}
