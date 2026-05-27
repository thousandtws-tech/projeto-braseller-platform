package com.example.application.service;

import com.example.application.exception.ForbiddenException;
import com.example.domain.model.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.Set;

@ApplicationScoped
public class TenantAuthorizationService {
    private static final Set<String> READ_ROLES = Set.of("ADMIN", "VENDEDOR", "CONTADOR");
    private static final Set<String> WRITE_ROLES = Set.of("ADMIN", "VENDEDOR");

    @Inject
    TenantContextService tenantContextService;

    public TenantContext requireReadable(String authorizationHeader) {
        TenantContext context = tenantContextService.resolve(authorizationHeader);
        requireAnyRole(context, READ_ROLES, "missing_required_role");
        return context;
    }

    public TenantContext requireWritable(String authorizationHeader) {
        TenantContext context = tenantContextService.resolve(authorizationHeader);
        requireAnyRole(context, WRITE_ROLES, "write_role_required");
        if (context.readOnly()) {
            throw new ForbiddenException("read_only_role");
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
