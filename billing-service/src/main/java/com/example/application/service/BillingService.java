package com.example.application.service;

import com.example.application.command.BillingWebhookCommand;
import com.example.application.command.ChangePlanCommand;
import com.example.application.command.StartTrialCommand;
import com.example.application.exception.NotFoundException;
import com.example.application.exception.ValidationException;
import com.example.application.port.out.BillingProviderGateway;
import com.example.application.port.out.BillingRepository;
import com.example.domain.model.BillingPlan;
import com.example.domain.model.BillingPlanCode;
import com.example.domain.model.BillingProvider;
import com.example.domain.model.BillingSubscription;
import com.example.domain.model.BillingWebhookEvent;
import com.example.domain.model.BillingWebhookEventType;
import com.example.domain.model.ProviderSubscriptionReference;
import com.example.domain.model.SubscriptionStatus;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class BillingService {
    private final BillingRepository repository;
    private final BillingProviderGateway billingProviderGateway;

    @Inject
    public BillingService(BillingRepository repository, BillingProviderGateway billingProviderGateway) {
        this.repository = repository;
        this.billingProviderGateway = billingProviderGateway;
    }

    public List<BillingPlan> plans() {
        return repository.listPlans();
    }

    public BillingSubscription subscription(String tenantId) {
        return repository.findSubscriptionByTenantId(requireText(tenantId, "tenantId"))
                .orElseThrow(() -> new NotFoundException("subscription_not_found"));
    }

    public BillingSubscription startTrial(StartTrialCommand command) {
        String tenantId = requireText(command.tenantId(), "tenantId");
        if (repository.findSubscriptionByTenantId(tenantId).isPresent()) {
            throw new ValidationException("subscription_already_exists");
        }
        BillingPlan plan = requireActivePlan(command.planCode() == null ? BillingPlanCode.BASIC : command.planCode());
        Instant now = Instant.now();
        ProviderSubscriptionReference reference = billingProviderGateway.createOrChangeSubscription(tenantId, plan);
        BillingSubscription subscription = new BillingSubscription(
                UUID.randomUUID().toString(),
                tenantId,
                plan.code(),
                SubscriptionStatus.TRIALING,
                reference.provider(),
                reference.providerCustomerId(),
                reference.providerSubscriptionId(),
                now,
                now.plus(Duration.ofDays(plan.trialDays())),
                now,
                now.plus(Duration.ofDays(plan.trialDays())),
                null,
                null,
                null,
                now,
                now
        );
        return repository.insertSubscription(subscription);
    }

    public BillingSubscription changePlan(ChangePlanCommand command) {
        String tenantId = requireText(command.tenantId(), "tenantId");
        BillingPlan plan = requireActivePlan(command.planCode());
        BillingSubscription current = subscription(tenantId);
        if (current.status() == SubscriptionStatus.CANCELLED) {
            throw new ValidationException("cancelled_subscription_cannot_change_plan");
        }
        if (current.planCode() == plan.code()) {
            return current;
        }
        ProviderSubscriptionReference reference = billingProviderGateway.createOrChangeSubscription(tenantId, plan);
        return repository.updateSubscription(current.withPlan(plan.code(), reference, Instant.now()));
    }

    public BillingSubscription applyWebhook(BillingWebhookCommand command) {
        BillingWebhookCommand safeCommand = normalize(command);
        String providerName = safeCommand.provider().name();
        if (repository.webhookEventExists(providerName, safeCommand.providerEventId())) {
            return subscription(safeCommand.tenantId());
        }

        Instant now = Instant.now();
        BillingSubscription current = repository.findSubscriptionByTenantId(safeCommand.tenantId())
                .orElseGet(() -> newWebhookSubscription(safeCommand, now));
        SubscriptionStatus status = statusFor(safeCommand.eventType());
        BillingSubscription updated = current.withWebhookStatus(
                status,
                safeCommand.planCode(),
                safeCommand.provider(),
                safeCommand.providerCustomerId(),
                safeCommand.providerSubscriptionId(),
                safeCommand.providerEventId(),
                safeCommand.reason(),
                now
        );

        BillingSubscription saved = repository.findSubscriptionByTenantId(safeCommand.tenantId()).isPresent()
                ? repository.updateSubscription(updated)
                : repository.insertSubscription(updated);
        repository.insertWebhookEvent(new BillingWebhookEvent(
                UUID.randomUUID().toString(),
                safeCommand.provider(),
                safeCommand.providerEventId(),
                safeCommand.tenantId(),
                safeCommand.eventType(),
                "PROCESSED",
                now,
                safeCommand.payload()
        ));
        return saved;
    }

    private BillingWebhookCommand normalize(BillingWebhookCommand command) {
        if (command == null) {
            throw new ValidationException("webhook_event is required");
        }
        return new BillingWebhookCommand(
                command.provider() == null ? BillingProvider.LOCAL : command.provider(),
                requireText(command.providerEventId(), "providerEventId"),
                command.eventType() == null ? null : command.eventType(),
                requireText(command.tenantId(), "tenantId"),
                command.planCode(),
                blankToNull(command.providerCustomerId()),
                blankToNull(command.providerSubscriptionId()),
                blankToNull(command.reason()),
                command.payload()
        );
    }

    private BillingSubscription newWebhookSubscription(BillingWebhookCommand command, Instant now) {
        BillingPlanCode planCode = command.planCode();
        if (planCode == null) {
            throw new ValidationException("plan_code is required for new webhook subscription");
        }
        requireActivePlan(planCode);
        return new BillingSubscription(
                UUID.randomUUID().toString(),
                command.tenantId(),
                planCode,
                statusFor(command.eventType()),
                command.provider(),
                command.providerCustomerId(),
                command.providerSubscriptionId(),
                null,
                null,
                now,
                now.plus(Duration.ofDays(30)),
                statusFor(command.eventType()) == SubscriptionStatus.SUSPENDED ? now : null,
                command.reason(),
                command.providerEventId(),
                now,
                now
        );
    }

    private SubscriptionStatus statusFor(BillingWebhookEventType eventType) {
        if (eventType == null) {
            throw new ValidationException("event_type is required");
        }
        return switch (eventType) {
            case SUBSCRIPTION_ACTIVATED, PAYMENT_SUCCEEDED -> SubscriptionStatus.ACTIVE;
            case PAYMENT_FAILED, SUBSCRIPTION_SUSPENDED -> SubscriptionStatus.SUSPENDED;
            case SUBSCRIPTION_CANCELLED -> SubscriptionStatus.CANCELLED;
        };
    }

    private BillingPlan requireActivePlan(BillingPlanCode planCode) {
        if (planCode == null) {
            throw new ValidationException("plan_code is required");
        }
        BillingPlan plan = repository.findPlan(planCode)
                .orElseThrow(() -> new ValidationException("plan_not_found"));
        if (!plan.active()) {
            throw new ValidationException("plan_inactive");
        }
        return plan;
    }

    private String requireText(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new ValidationException(fieldName + " is required");
        }
        return value.trim();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
