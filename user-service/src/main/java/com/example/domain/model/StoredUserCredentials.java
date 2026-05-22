package com.example.domain.model;

import java.util.List;

public record StoredUserCredentials(String userId, String tenantId, String email, String fullName, String passwordHash,
                                    List<String> roles) {
}
