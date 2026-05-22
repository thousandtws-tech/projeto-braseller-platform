package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "AuthTokenSet", description = "Tokens e contexto principal retornados pelo auth-service.")
public record AuthTokenSet(String accessToken, String refreshToken, String tokenType, String expiresAt, String tenantId,
                           String userId, String email, List<String> roles) {
}
