package com.example.domain.model;

import java.time.Instant;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RegistrationResult", description = "Resultado do cadastro inicial antes da ativacao do e-mail.")
public record RegistrationResult(String email, String status, boolean verificationRequired, Instant verificationExpiresAt) {
}
