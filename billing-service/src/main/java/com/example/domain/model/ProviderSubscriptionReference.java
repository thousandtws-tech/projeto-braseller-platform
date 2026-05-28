package com.example.domain.model;

public record ProviderSubscriptionReference(
        BillingProvider provider,
        String providerCustomerId,
        String providerSubscriptionId) {
}
