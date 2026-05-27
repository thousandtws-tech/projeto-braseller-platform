package com.example.domain.model;

public record KeycloakTokenResponse(String accessToken, String idToken, String refreshToken, String tokenType, long expiresIn) {
}
