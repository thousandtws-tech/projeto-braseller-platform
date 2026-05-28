package com.example.application.port.out;

import com.example.domain.model.BillingPlan;
import com.example.domain.model.BillingPlanCode;
import com.example.domain.model.BillingSubscription;
import com.example.domain.model.BillingWebhookEvent;

import java.util.List;
import java.util.Optional;

public interface BillingRepository {
    List<BillingPlan> listPlans();

    Optional<BillingPlan> findPlan(BillingPlanCode code);

    Optional<BillingSubscription> findSubscriptionByTenantId(String tenantId);

    BillingSubscription insertSubscription(BillingSubscription subscription);

    BillingSubscription updateSubscription(BillingSubscription subscription);

    boolean webhookEventExists(String provider, String providerEventId);

    void insertWebhookEvent(BillingWebhookEvent event);
}
