package com.example.domain.model.connector;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

public record ConnectorDescriptor(
        @JsonProperty("name") String name,
        @JsonProperty("display_name") String displayName,
        @JsonProperty("supports_invoices") boolean supportsInvoices,
        @JsonProperty("required_methods") List<String> requiredMethods,
        @JsonProperty("optional_methods") List<String> optionalMethods) {

    public ConnectorDescriptor {
        requiredMethods = requiredMethods == null ? List.of() : List.copyOf(requiredMethods);
        optionalMethods = optionalMethods == null ? List.of() : List.copyOf(optionalMethods);
    }
}
