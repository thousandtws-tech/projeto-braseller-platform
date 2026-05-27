package com.example.domain.model;

import java.util.List;

public record AuthIdentity(String id, String tenantId, String userId, String email, String fullName, List<String> roles,
                           String status, String provider, String providerSubject, String preferredUsername,
                           String firstName, String lastName, String pictureUrl, boolean emailVerified) {
    public AuthIdentity(String id, String tenantId, String userId, String email, String fullName, List<String> roles,
                        String status) {
        this(id, tenantId, userId, email, fullName, roles, status, "USER_SERVICE", userId, email, null, null, null, true);
    }
}
