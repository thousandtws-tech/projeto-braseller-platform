package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AccountantClientView", description = "Cliente/tenant atendido por um contador no painel BPO.")
public record AccountantClientView(
        String tenantId,
        String legalName,
        String tradeName,
        String tenantStatus,
        boolean readOnly,
        String accessStatus,
        Instant grantedAt
) {
}
