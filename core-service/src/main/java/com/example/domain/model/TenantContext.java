package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "TenantContext", description = "Contexto extraido do JWT para isolamento multi-tenant.")
public record TenantContext(String tenantId, String userId, String email, List<String> roles, boolean readOnly) {
}
