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

    private static final Set<String> ALLOWED_ROLES = Set.of("ADMIN", "VENDEDOR", "CONTADOR");

    @Inject
    AccessTokenVerifier accessTokenVerifier;

    public TenantContext requireTenant(String authorizationHeader, String tenantId) {
        TenantContext context = accessTokenVerifier.verify(authorizationHeader);
        if (!context.tenantId().equals(tenantId)) {
            throw new TenantMismatchException();
        }
        boolean allowed = context.roles().stream().anyMatch(ALLOWED_ROLES::contains);
        if (!allowed) {
            throw new ForbiddenException("missing_required_role");
        }
        return context;
    }

    public TenantContext requireAdmin(String authorizationHeader, String tenantId) {
        TenantContext context = requireTenant(authorizationHeader, tenantId);
        if (!context.roles().contains("ADMIN")) {
            throw new ForbiddenException("admin_role_required");
        }
        return context;
    }
}
