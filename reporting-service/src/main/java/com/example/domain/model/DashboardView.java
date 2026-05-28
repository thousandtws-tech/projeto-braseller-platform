package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record DashboardView(
        @JsonProperty("summary") FinancialSummary summary,
        @JsonProperty("entries") ReportEntryPage entries,
        @JsonProperty("monthly_evolution") List<MonthlyEvolutionPoint> monthlyEvolution,
        @JsonProperty("platform_comparison") List<PlatformComparisonPoint> platformComparison,
        @JsonProperty("filters") AvailableFilters filters) {
    public DashboardView {
        monthlyEvolution = monthlyEvolution == null ? List.of() : List.copyOf(monthlyEvolution);
        platformComparison = platformComparison == null ? List.of() : List.copyOf(platformComparison);
    }
}
