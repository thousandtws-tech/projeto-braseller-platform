package com.example.domain.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.Instant;

public record BillingSubscription(
        @JsonProperty("id") String id,
        @JsonProperty("tenant_id") String tenantId,
        @JsonProperty("plan_code") BillingPlanCode planCode,
        @JsonProperty("status") SubscriptionStatus status,
        @JsonProperty("provider") BillingProvider provider,
        @JsonProperty("provider_customer_id") String providerCustomerId,
        @JsonProperty("provider_subscription_id") String providerSubscriptionId,
        @JsonProperty("trial_started_at") Instant trialStartedAt,
        @JsonProperty("trial_ends_at") Instant trialEndsAt,
        @JsonProperty("current_period_started_at") Instant currentPeriodStartedAt,
        @JsonProperty("current_period_ends_at") Instant currentPeriodEndsAt,
        @JsonProperty("suspended_at") Instant suspendedAt,
        @JsonProperty("cancellation_reason") String cancellationReason,
        @JsonProperty("latest_event_id") String latestEventId,
        @JsonProperty("created_at") Instant createdAt,
        @JsonProperty("updated_at") Instant updatedAt) {

    @JsonProperty("access_enabled")
    public boolean accessEnabled() {
        return status != null && status.accessEnabled();
    }

    public BillingSubscription withPlan(BillingPlanCode nextPlan, ProviderSubscriptionReference reference, Instant now) {
        return new BillingSubscription(
                id,
                tenantId,
                nextPlan,
                status,
                reference.provider(),
                reference.providerCustomerId(),
                reference.providerSubscriptionId(),
                trialStartedAt,
                trialEndsAt,
                currentPeriodStartedAt,
                currentPeriodEndsAt,
                suspendedAt,
                cancellationReason,
                latestEventId,
                createdAt,
                now
        );
    }

    public BillingSubscription withWebhookStatus(
            SubscriptionStatus nextStatus,
            BillingPlanCode nextPlan,
            BillingProvider nextProvider,
            String nextProviderCustomerId,
            String nextProviderSubscriptionId,
            String nextEventId,
            String reason,
            Instant now) {
        boolean suspended = nextStatus == SubscriptionStatus.SUSPENDED || nextStatus == SubscriptionStatus.CANCELLED;
        return new BillingSubscription(
                id,
                tenantId,
                nextPlan == null ? planCode : nextPlan,
                nextStatus,
                nextProvider == null ? provider : nextProvider,
                nextProviderCustomerId == null ? providerCustomerId : nextProviderCustomerId,
                nextProviderSubscriptionId == null ? providerSubscriptionId : nextProviderSubscriptionId,
                trialStartedAt,
                trialEndsAt,
                currentPeriodStartedAt,
                currentPeriodEndsAt,
                suspended ? now : null,
                reason == null ? cancellationReason : reason,
                nextEventId,
                createdAt,
                now
        );
    }
}
