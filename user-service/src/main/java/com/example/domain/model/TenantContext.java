package com.example.domain.model;

import java.util.List;

public record TenantContext(String tenantId, String userId, String email, List<String> roles, boolean readOnly) {
}
