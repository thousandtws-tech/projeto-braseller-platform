package com.example.domain.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AuthTokenSet", description = "Tokens e contexto principal retornados pelo auth-service.")
public record AuthTokenSet(String accessToken, String refreshToken, String tokenType, String expiresAt, String tenantId,
        String userId, String email, List<String> roles, AuthProfile profile) {
}
