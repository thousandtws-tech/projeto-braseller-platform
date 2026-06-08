package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "TenantView", description = "Representacao publica de um tenant.")
public record TenantView(
        String id,
        String legalName,
        String tradeName,
        String status,
        String cnpj,
        String cnaeCode,
        String cnaeDescription,
        String addressStreet,
        String addressNumber,
        String addressComplement,
        String addressNeighborhood,
        String addressCity,
        String addressState,
        String addressZipCode
) {
}
