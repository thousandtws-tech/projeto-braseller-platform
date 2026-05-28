package com.example.domain.model;

public enum SubscriptionStatus {
    TRIALING,
    ACTIVE,
    PAST_DUE,
    SUSPENDED,
    CANCELLED;

    public boolean accessEnabled() {
        return this == TRIALING || this == ACTIVE;
    }
}
