package com.example.domain.model;

import com.example.application.exception.ValidationException;

import java.util.Locale;

public enum BillingPlanCode {
    BASIC,
    PRO,
    AGENCY;

    public static BillingPlanCode parse(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("plan_code is required");
        }
        String normalized = value.trim()
                .replace('-', '_')
                .replace(' ', '_')
                .toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BASIC", "BASICO" -> BASIC;
            case "PRO" -> PRO;
            case "AGENCY", "AGENCIA" -> AGENCY;
            default -> throw new ValidationException("invalid_plan_code");
        };
    }
}
