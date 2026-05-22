package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "RegisteredTenant", description = "Tenant criado e usuario administrador inicial.")
public record RegisteredTenant(TenantView tenant, UserView adminUser) {
}
