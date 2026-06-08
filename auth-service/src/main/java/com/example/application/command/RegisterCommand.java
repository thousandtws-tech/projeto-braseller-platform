package com.example.application.command;

public record RegisterCommand(
        String tenantName,
        String fullName,
        String email,
        String password,
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
        String addressZipCode
) {
}
