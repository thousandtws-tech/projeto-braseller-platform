package com.example.application.command;

import com.example.domain.model.BillingPlanCode;
import com.example.domain.model.BillingProvider;
import com.example.domain.model.BillingWebhookEventType;

public record BillingWebhookCommand(
        BillingProvider provider,
        String providerEventId,
        BillingWebhookEventType eventType,
        String tenantId,
        BillingPlanCode planCode,
        String providerCustomerId,
        String providerSubscriptionId,
        String reason,
        String payload) {
}
