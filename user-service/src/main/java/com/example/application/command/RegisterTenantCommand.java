package com.example.application.command;

public record RegisterTenantCommand(
        String legalName,
        String tradeName,
        String adminName,
        String email,
        String password,
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
