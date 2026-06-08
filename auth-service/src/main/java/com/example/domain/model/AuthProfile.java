package com.example.domain.model;

import java.util.List;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AuthProfile", description = "Dados de perfil do usuario autenticado.")
public record AuthProfile(String provider, String subject, String tenantId, String userId, String email,
                String fullName,
                String preferredUsername, String firstName, String lastName, String pictureUrl,
                boolean emailVerified, List<String> roles) {

}
