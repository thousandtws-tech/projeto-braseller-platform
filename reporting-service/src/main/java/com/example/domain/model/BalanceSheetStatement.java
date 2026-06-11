package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;

public record BalanceSheetStatement(
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("as_of") LocalDate asOf,
        @JsonProperty("cash_and_bank") BigDecimal cashAndBank,
        @JsonProperty("accounts_receivable") BigDecimal accountsReceivable,
        @JsonProperty("inventory") BigDecimal inventory,
        @JsonProperty("total_assets") BigDecimal totalAssets,
        @JsonProperty("accounts_payable") BigDecimal accountsPayable,
        @JsonProperty("taxes_payable") BigDecimal taxesPayable,
        @JsonProperty("total_liabilities") BigDecimal totalLiabilities,
        @JsonProperty("equity") BigDecimal equity,
        @JsonProperty("accumulated_net_result") BigDecimal accumulatedNetResult,
        @JsonProperty("total_liabilities_and_equity") BigDecimal totalLiabilitiesAndEquity) {
}
