package com.example.domain.model;

import java.time.LocalDate;
import java.util.Locale;
import java.util.Set;

public record ReportFilter(
        LocalDate from,
        LocalDate to,
        String platform,
        PaymentMethod paymentMethod,
        ReportEntryStatus status,
        String search,
        String sort,
        String direction,
        Integer page,
        Integer size) {
    private static final Set<String> SORT_FIELDS = Set.of(
            "sale_date",
            "platform",
            "payment_method",
            "status",
            "gross_value",
            "received_value",
            "fee_value",
            "receivable_value",
            "order_id",
            "buyer_name"
    );

    public int safePage() {
        return page == null ? 0 : Math.max(0, page);
    }

    public int safeSize() {
        return size == null ? 50 : Math.max(1, Math.min(size, 100));
    }

    public int offset() {
        return safePage() * safeSize();
    }

    public String normalizedPlatform() {
        return platform == null || platform.isBlank() ? null : platform.trim().toLowerCase(Locale.ROOT);
    }

    public String normalizedSearch() {
        return search == null || search.isBlank() ? null : search.trim().toLowerCase(Locale.ROOT);
    }

    public String sortColumn() {
        String normalized = normalizeSort(sort);
        return SORT_FIELDS.contains(normalized) ? normalized : "sale_date";
    }

    public String sortDirection() {
        return "ASC".equalsIgnoreCase(direction) ? "ASC" : "DESC";
    }

    private String normalizeSort(String value) {
        if (value == null || value.isBlank()) {
            return "sale_date";
        }
        String normalized = value.trim()
                .replaceAll("([a-z])([A-Z])", "$1_$2")
                .replace('-', '_')
                .toLowerCase(Locale.ROOT);
        return SORT_FIELDS.contains(normalized) ? normalized : "sale_date";
    }
}
