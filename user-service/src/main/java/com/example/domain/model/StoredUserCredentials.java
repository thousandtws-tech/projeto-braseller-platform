package com.example.domain.model;

import java.util.List;

public record StoredUserCredentials(String userId, String tenantId, String email, String fullName,
                                    String preferredUsername, String firstName, String lastName, String pictureUrl,
                                    boolean emailVerified, String provider, String providerSubject, String status, String passwordHash,
                                    List<String> roles) {
}
