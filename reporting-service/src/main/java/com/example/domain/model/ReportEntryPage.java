package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ReportEntryPage(
        @JsonProperty("items") List<ReportEntry> items,
        @JsonProperty("total") long total,
        @JsonProperty("page") int page,
        @JsonProperty("size") int size) {
    public ReportEntryPage {
        items = items == null ? List.of() : List.copyOf(items);
    }
}
