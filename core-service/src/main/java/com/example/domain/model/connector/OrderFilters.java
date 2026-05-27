package com.example.domain.model.connector;

import java.time.LocalDate;

public record OrderFilters(LocalDate from, LocalDate to, OrderStatus status, int limit) {
    public OrderFilters {
        limit = limit <= 0 ? 50 : Math.min(limit, 200);
    }
}
