package com.example.domain.model;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public record ReportExportData(
        String tenantId,
        String title,
        String periodLabel,
        Instant generatedAt,
        FinancialSummary summary,
        List<PlatformComparisonPoint> platformSummaries,
        List<ReportEntry> entries) {
    public ReportExportData {
        platformSummaries = platformSummaries == null ? List.of() : List.copyOf(platformSummaries);
        entries = entries == null ? List.of() : List.copyOf(entries);
    }

    public Map<String, List<ReportEntry>> entriesByPlatform() {
        return entries.stream()
                .sorted(Comparator.comparing(ReportEntry::platform)
                        .thenComparing(ReportEntry::saleDate)
                        .thenComparing(ReportEntry::orderId))
                .collect(Collectors.groupingBy(
                        ReportEntry::platform,
                        LinkedHashMap::new,
                        Collectors.toList()
                ));
    }
}
