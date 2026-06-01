package com.example.domain.model;

import java.time.LocalDate;

public record ExpenseFilter(
        LocalDate from,
        LocalDate to,
        ExpenseCategory category,
        Integer page,
        Integer size) {
    public int safePage() {
        return page == null ? 0 : Math.max(0, page);
    }

    public int safeSize() {
        return size == null ? 50 : Math.max(1, Math.min(size, 100));
    }

    public int offset() {
        return safePage() * safeSize();
    }
}
