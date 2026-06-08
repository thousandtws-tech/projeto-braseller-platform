package com.example.application.service;

import com.example.application.exception.ForbiddenException;
import com.example.application.port.out.AccessTokenVerifier;
import com.example.domain.model.TenantContext;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class TenantAuthorizationService {
    private static final Set<String> READ_ROLES = Set.of("ADMIN", "VENDEDOR", "CONTADOR", "BPO_ADMIN");
    private static final Set<String> WRITE_ROLES = Set.of("ADMIN", "VENDEDOR");
    private static final Set<String> INTEGRATION_WRITE_ROLES = Set.of("ADMIN", "VENDEDOR", "CONTADOR");
    private static final Set<String> CLOSING_SIGNER_ROLES = Set.of("CONTADOR", "BPO_ADMIN");

    @Inject
    AccessTokenVerifier accessTokenVerifier;

    public TenantContext requireReadable(String authorizationHeader, String tenantId) {
        TenantContext context = accessTokenVerifier.verify(authorizationHeader);
        requireAnyRole(context, READ_ROLES, "missing_required_role");
        requireReadableTenant(context, tenantId);
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

    public TenantContext requireClosingSigner(String authorizationHeader, String tenantId) {
        TenantContext context = accessTokenVerifier.verify(authorizationHeader);
        requireAnyRole(context, CLOSING_SIGNER_ROLES, "accountant_role_required");
        requireReadableTenant(context, tenantId);
        return context;
    }

    public TenantContext requireBatchClosingSigner(String authorizationHeader, List<String> tenantIds) {
        TenantContext context = accessTokenVerifier.verify(authorizationHeader);
        requireAnyRole(context, CLOSING_SIGNER_ROLES, "accountant_role_required");
        for (String tenantId : tenantIds) {
            requireReadableTenant(context, tenantId);
        }
        return context;
    }

    private void requireSameTenant(TenantContext context, String tenantId) {
        if (!context.tenantId().equals(tenantId)) {
            throw new ForbiddenException("tenant_mismatch");
        }
    }

    private void requireReadableTenant(TenantContext context, String tenantId) {
        if (context.tenantId().equals(tenantId)) {
            return;
        }
        if (isGlobalBpoOperator(context)) {
            return;
        }
        boolean accountantClient = context.roles().contains("CONTADOR")
                && context.accountantTenantIds().contains(tenantId);
        if (!accountantClient) {
            throw new ForbiddenException("tenant_mismatch");
        }
    }

    private void requireAnyRole(TenantContext context, Set<String> allowedRoles, String message) {
        boolean allowed = context.roles().stream().anyMatch(allowedRoles::contains);
        if (!allowed) {
            throw new ForbiddenException(message);
        }
    }

    private boolean isGlobalBpoOperator(TenantContext context) {
        return context.roles().contains("BPO_ADMIN")
                || (context.roles().contains("ADMIN") && context.roles().contains("CONTADOR"));
    }
}
