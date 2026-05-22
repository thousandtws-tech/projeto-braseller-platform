package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "IdentityVerification", description = "Identidade validada pelo user-service para o auth-service.")
public record IdentityVerification(String userId, String tenantId, String email, String fullName, List<String> roles) {
}
