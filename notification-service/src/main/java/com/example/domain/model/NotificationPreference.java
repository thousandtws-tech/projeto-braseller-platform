package com.example.domain.model;

import java.time.Instant;

public record NotificationPreference(
        String tenantId,
        boolean emailEnabled,
        boolean newSaleEnabled,
        boolean monthlyClosingEnabled,
        boolean mlPaymentReleaseEnabled,
        boolean weeklyAccountantReportEnabled,
        String recipientEmail,
        String accountantEmail,
        Instant updatedAt) {

    public static NotificationPreference defaults(String tenantId) {
        return new NotificationPreference(
                tenantId,
                true,
                false,
                true,
                true,
                true,
                null,
                null,
                Instant.now()
        );
    }
}
