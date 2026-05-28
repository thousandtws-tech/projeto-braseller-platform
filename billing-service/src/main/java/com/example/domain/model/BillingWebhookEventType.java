package com.example.domain.model;

import com.example.application.exception.ValidationException;

import java.util.Locale;

public enum BillingWebhookEventType {
    SUBSCRIPTION_ACTIVATED,
    PAYMENT_SUCCEEDED,
    PAYMENT_FAILED,
    SUBSCRIPTION_SUSPENDED,
    SUBSCRIPTION_CANCELLED;

    public static BillingWebhookEventType parse(String value) {
        if (value == null || value.isBlank()) {
            throw new ValidationException("event_type is required");
        }
        String normalized = value.trim().replace('-', '_').replace('.', '_').toUpperCase(Locale.ROOT);
        try {
            return BillingWebhookEventType.valueOf(normalized);
        } catch (IllegalArgumentException exception) {
            throw new ValidationException("invalid_event_type");
        }
    }
}
