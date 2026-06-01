package com.example.application.command;

import com.example.domain.model.TaxRegime;

import java.math.BigDecimal;

public record UpsertFiscalProfileCommand(
        String tenantId,
        TaxRegime taxRegime,
        BigDecimal estimatedTaxRate,
        String notes) {
}
