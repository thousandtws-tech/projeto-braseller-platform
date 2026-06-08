package com.example.domain.model;

import java.util.List;

public record TenantContext(String tenantId, String userId, String email, List<String> roles, boolean readOnly,
                            List<String> accountantTenantIds) {
    public TenantContext {
        roles = roles == null ? List.of() : List.copyOf(roles);
        accountantTenantIds = accountantTenantIds == null ? List.of() : List.copyOf(accountantTenantIds);
    }
}
