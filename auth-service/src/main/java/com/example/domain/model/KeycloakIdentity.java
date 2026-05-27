package com.example.domain.model;

public record KeycloakIdentity(String subject, String email, String fullName, boolean emailVerified,
                               String preferredUsername, String firstName, String lastName, String pictureUrl) {
}
