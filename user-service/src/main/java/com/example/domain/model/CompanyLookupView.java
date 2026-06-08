package com.example.domain.model;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CompanyLookupView", description = "Dados retornados da consulta publica de CNPJ.")
public record CompanyLookupView(
        String cnpj,
        String legalName,
        String tradeName,
        String cnaeCode,
        String cnaeDescription,
        String addressStreet,
        String addressNumber,
        String addressComplement,
        String addressNeighborhood,
        String addressCity,
        String addressState,
        String addressZipCode,
        String registrationStatus,
        String email,
        String phone,
        String source
) {
}
