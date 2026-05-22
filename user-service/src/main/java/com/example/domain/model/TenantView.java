package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "TenantView", description = "Representacao publica de um tenant.")
public record TenantView(String id, String legalName, String tradeName, String status) {
}
