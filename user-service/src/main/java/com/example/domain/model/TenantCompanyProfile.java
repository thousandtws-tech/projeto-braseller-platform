package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "TenantCompanyProfile", description = "Dados cadastrais fiscais basicos do tenant.")
public record TenantCompanyProfile(
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
