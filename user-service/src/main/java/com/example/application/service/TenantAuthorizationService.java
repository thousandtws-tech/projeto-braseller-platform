package com.example.application.service;

import com.example.application.exception.ForbiddenException;
import com.example.application.exception.TenantMismatchException;
import com.example.application.port.out.AccessTokenVerifier;
import com.example.domain.model.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

@ApplicationScoped
public class TenantAuthorizationService {
    private static final Set<String> READ_ROLES = Set.of("ADMIN", "VENDEDOR", "CONTADOR");

    @Inject
    AccessTokenVerifier accessTokenVerifier;

    public TenantContext requireTenant(String authorizationHeader, String tenantId) {
        TenantContext context = accessTokenVerifier.verify(authorizationHeader);
        if (!context.tenantId().equals(tenantId)) {
            throw new TenantMismatchException();
        }
        requireAnyRole(context, READ_ROLES, "missing_required_role");
        return context;
    }

    public TenantContext requireAdmin(String authorizationHeader, String tenantId) {
        TenantContext context = requireTenant(authorizationHeader, tenantId);
        if (!context.roles().contains("ADMIN")) {
            throw new ForbiddenException("admin_role_required");
        }
        return context;
    }

    private void requireAnyRole(TenantContext context, Set<String> allowedRoles, String message) {
        boolean allowed = context.roles().stream().anyMatch(allowedRoles::contains);
        if (!allowed) {
            throw new ForbiddenException(message);
        }
    }
}
