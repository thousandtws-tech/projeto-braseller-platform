package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "UserView", description = "Representacao publica de usuario e seus papeis no tenant.")
public record UserView(String id, String tenantId, String email, String fullName, String preferredUsername,
                       String firstName, String lastName, String pictureUrl, boolean emailVerified, String provider,
                       String providerSubject, String status, List<String> roles, List<String> accountantTenantIds) {
    public UserView {
        roles = roles == null ? List.of() : List.copyOf(roles);
        accountantTenantIds = accountantTenantIds == null ? List.of() : List.copyOf(accountantTenantIds);
    }
}
