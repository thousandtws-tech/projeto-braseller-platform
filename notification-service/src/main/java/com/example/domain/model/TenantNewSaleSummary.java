package com.example.domain.model;

import io.quarkus.runtime.annotations.RegisterForReflection;

import java.math.BigDecimal;
import java.time.Instant;

@RegisterForReflection
public record TenantNewSaleSummary(
        String tenantId,
        long saleCount,
        BigDecimal grossRevenue,
        String lastMarketplace,
        String lastOrderId,
        String lastEventId,
        Instant lastEventAt) {

    public static TenantNewSaleSummary empty() {
        return empty(null);
    }

    public static TenantNewSaleSummary empty(String tenantId) {
        return new TenantNewSaleSummary(tenantId, 0, BigDecimal.ZERO, null, null, null, null);
    }

    public TenantNewSaleSummary add(String tenantId, String marketplace, String orderId, String eventId, BigDecimal amount, Instant occurredAt) {
        BigDecimal currentRevenue = grossRevenue == null ? BigDecimal.ZERO : grossRevenue;
        BigDecimal nextAmount = amount == null ? BigDecimal.ZERO : amount;
        return new TenantNewSaleSummary(
                tenantId,
                saleCount + 1,
                currentRevenue.add(nextAmount),
                marketplace,
                orderId,
                eventId,
                occurredAt
        );
    }
}
