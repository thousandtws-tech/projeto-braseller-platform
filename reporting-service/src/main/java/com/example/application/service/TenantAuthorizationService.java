package com.example.application.service;

import com.example.application.exception.ForbiddenException;
import com.example.application.port.out.AccessTokenVerifier;
import com.example.domain.model.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

@ApplicationScoped
public class TenantAuthorizationService {
    private static final Set<String> READ_ROLES = Set.of("ADMIN", "VENDEDOR", "CONTADOR");
    private static final Set<String> WRITE_ROLES = Set.of("ADMIN", "VENDEDOR");
    private static final Set<String> INTEGRATION_WRITE_ROLES = Set.of("ADMIN", "VENDEDOR", "CONTADOR");

    @Inject
    AccessTokenVerifier accessTokenVerifier;

    public TenantContext requireReadable(String authorizationHeader, String tenantId) {
        TenantContext context = accessTokenVerifier.verify(authorizationHeader);
        requireSameTenant(context, tenantId);
        requireAnyRole(context, READ_ROLES, "missing_required_role");
        return context;
    }

    public TenantContext requireWritable(String authorizationHeader, String tenantId) {
        TenantContext context = accessTokenVerifier.verify(authorizationHeader);
        requireSameTenant(context, tenantId);
        requireAnyRole(context, WRITE_ROLES, "write_role_required");
        if (context.readOnly()) {
            throw new ForbiddenException("read_only_role");
        }
        return context;
    }

    public TenantContext requireIntegrationWritable(String authorizationHeader, String tenantId) {
        TenantContext context = accessTokenVerifier.verify(authorizationHeader);
        requireSameTenant(context, tenantId);
        requireAnyRole(context, INTEGRATION_WRITE_ROLES, "integration_role_required");
        return context;
    }

    private void requireSameTenant(TenantContext context, String tenantId) {
        if (!context.tenantId().equals(tenantId)) {
            throw new ForbiddenException("tenant_mismatch");
        }
    }

    private void requireAnyRole(TenantContext context, Set<String> allowedRoles, String message) {
        boolean allowed = context.roles().stream().anyMatch(allowedRoles::contains);
        if (!allowed) {
            throw new ForbiddenException(message);
        }
    }
}
