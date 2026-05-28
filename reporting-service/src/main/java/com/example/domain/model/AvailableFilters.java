package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record AvailableFilters(
        @JsonProperty("platforms") List<String> platforms,
        @JsonProperty("payment_methods") List<PaymentMethod> paymentMethods,
        @JsonProperty("statuses") List<ReportEntryStatus> statuses) {
    public AvailableFilters {
        platforms = platforms == null ? List.of() : List.copyOf(platforms);
        paymentMethods = paymentMethods == null ? List.of() : List.copyOf(paymentMethods);
        statuses = statuses == null ? List.of() : List.copyOf(statuses);
    }
}
