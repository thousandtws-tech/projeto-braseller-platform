package com.example.infrastructure.billing;

import com.example.application.port.out.BillingProviderGateway;
import com.example.domain.model.BillingPlan;
import com.example.domain.model.BillingProvider;
import com.example.domain.model.ProviderSubscriptionReference;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.Locale;

@ApplicationScoped
public class LocalBillingProviderGateway implements BillingProviderGateway {
    @Override
    public ProviderSubscriptionReference createOrChangeSubscription(String tenantId, BillingPlan plan) {
        String normalizedTenant = tenantId.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9_-]", "-");
        return new ProviderSubscriptionReference(
                BillingProvider.LOCAL,
                "local_customer_" + normalizedTenant,
                "local_subscription_" + normalizedTenant + "_" + plan.code().name().toLowerCase(Locale.ROOT)
        );
    }
}
