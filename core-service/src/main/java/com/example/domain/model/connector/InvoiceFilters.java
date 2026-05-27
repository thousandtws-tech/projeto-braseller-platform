package com.example.domain.model.connector;

import java.time.LocalDate;

public record InvoiceFilters(LocalDate from, LocalDate to, int limit) {
    public InvoiceFilters {
        limit = limit <= 0 ? 50 : Math.min(limit, 200);
    }
}
