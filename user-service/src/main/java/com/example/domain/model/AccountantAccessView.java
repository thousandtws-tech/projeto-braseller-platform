package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AccountantAccessView", description = "Acesso secundario do contador ao tenant.")
public record AccountantAccessView(String id, String tenantId, String accountantUserId, String email, boolean readOnly,
                                   String status) {
}
