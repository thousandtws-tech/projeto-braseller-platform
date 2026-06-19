package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "EmailVerificationResult", description = "Resultado da verificacao de e-mail.")
public record EmailVerificationResult(String email, String status, boolean emailVerified) {
}
