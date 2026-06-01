package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ExpensePage(
        @JsonProperty("items") List<ExpenseEntry> items,
        @JsonProperty("total") long total,
        @JsonProperty("page") int page,
        @JsonProperty("size") int size) {
    public ExpensePage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
