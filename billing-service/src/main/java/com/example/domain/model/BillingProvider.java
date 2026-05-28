package com.example.domain.model;

import java.util.Locale;

public enum BillingProvider {
    LOCAL,
    STRIPE,
    PAGARME;

    public static BillingProvider parseOrLocal(String value) {
        if (value == null || value.isBlank()) {
            return LOCAL;
        }
        String normalized = value.trim().replace(".", "").replace("-", "").replace("_", "").toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "STRIPE" -> STRIPE;
            case "PAGARME", "PAGAR" -> PAGARME;
            default -> LOCAL;
        };
    }
}
