package com.example.infrastructure.client.dto;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.math.BigDecimal;

public record ReportingSummaryResponse(
        @JsonProperty("gross_value") BigDecimal grossValue,
        @JsonProperty("entry_count") long entryCount) {
}
