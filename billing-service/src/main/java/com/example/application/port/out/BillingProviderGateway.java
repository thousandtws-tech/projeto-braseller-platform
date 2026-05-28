package com.example.application.port.out;

import com.example.domain.model.BillingPlan;
import com.example.domain.model.ProviderSubscriptionReference;

public interface BillingProviderGateway {
    ProviderSubscriptionReference createOrChangeSubscription(String tenantId, BillingPlan plan);
}
