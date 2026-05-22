package com.example.domain.model;

import java.util.List;

public record AuthIdentity(String id, String tenantId, String userId, String email, String fullName, List<String> roles,
                           String status) {
}
