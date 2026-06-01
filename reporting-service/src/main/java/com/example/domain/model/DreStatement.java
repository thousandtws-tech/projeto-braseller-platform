package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record DreStatement(
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("from") LocalDate from,
        @JsonProperty("to") LocalDate to,
        @JsonProperty("tax_regime") TaxRegime taxRegime,
        @JsonProperty("estimated_tax_rate") BigDecimal estimatedTaxRate,
        @JsonProperty("gross_revenue") BigDecimal grossRevenue,
        @JsonProperty("marketplace_fees") BigDecimal marketplaceFees,
        @JsonProperty("estimated_taxes") BigDecimal estimatedTaxes,
        @JsonProperty("operating_expenses") BigDecimal operatingExpenses,
        @JsonProperty("net_result") BigDecimal netResult,
        @JsonProperty("sales_count") long salesCount,
        @JsonProperty("expense_count") long expenseCount,
        @JsonProperty("expenses_by_category") List<ExpenseCategoryTotal> expensesByCategory) {
    public DreStatement {
        expensesByCategory = expensesByCategory == null ? List.of() : List.copyOf(expensesByCategory);
    }
}
