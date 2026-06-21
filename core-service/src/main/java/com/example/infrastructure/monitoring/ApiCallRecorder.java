package com.example.infrastructure.monitoring;

import com.example.application.port.out.ApiIntegrationEventRepository;
import com.example.application.port.out.ApiIntegrationStatusRepository;
import com.example.domain.enums.ApiCallOutcome;
import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;
import com.example.domain.model.monitoring.NewApiIntegrationEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.time.Instant;
import java.util.function.Supplier;

@ApplicationScoped
public class ApiCallRecorder {
    private static final int RATE_LIMIT_CRITICAL_STREAK = 5;
    private static final int TIMEOUT_CRITICAL_STREAK = 3;

    @Inject
    ApiIntegrationEventRepository eventRepository;

    @Inject
    ApiIntegrationStatusRepository statusRepository;

    @Inject
    ApiFailureClassifier classifier;

    @Inject
    ApiIntegrationAlertPublisher alertPublisher;

    public <T> T record(ApiCallContext context, Supplier<T> call) {
        long startNanos = System.nanoTime();
        try {
            T result = call.get();
            recordSuccess(context, elapsedMillis(startNanos));
            return result;
        } catch (RuntimeException exception) {
            recordFailure(context, elapsedMillis(startNanos), exception);
            throw exception;
        }
    }

    public void recordVoid(ApiCallContext context, Runnable call) {
        record(context, () -> {
            call.run();
            return null;
        });
    }

    private void recordSuccess(ApiCallContext context, int elapsedMs) {
        statusRepository.applySuccess(context.tenantId(), context.integrationName(), elapsedMs);
        eventRepository.record(new NewApiIntegrationEvent(
                context.tenantId(),
                context.integrationName(),
                context.endpoint(),
                context.operation(),
                Instant.now(),
                elapsedMs,
                null,
                ApiCallOutcome.SUCCESS,
                null,
                ApiSeverity.INFO,
                null,
                null,
                null
        ));
    }

    private void recordFailure(ApiCallContext context, int elapsedMs, RuntimeException exception) {
        ApiFailureClassifier.Classification classification = classifier.classify(exception, context.failureTypeHint());
        int consecutiveFailures = statusRepository.applyFailure(
                context.tenantId(), context.integrationName(), elapsedMs,
                classification.failureType(), classification.severity());

        ApiSeverity severity = escalate(classification, consecutiveFailures);

        eventRepository.record(new NewApiIntegrationEvent(
                context.tenantId(),
                context.integrationName(),
                context.endpoint(),
                context.operation(),
                Instant.now(),
                elapsedMs,
                classification.httpStatus(),
                ApiCallOutcome.FAILURE,
                classification.failureType(),
                severity,
                classification.impact(),
                classification.actionTaken(),
                truncate(exception.getMessage(), 1000)
        ));

        if (severity == ApiSeverity.CRITICAL) {
            alertPublisher.publishCriticalAlert(context, context.integrationName(),
                    classification.failureType(), severity, classification.impact(), classification.actionTaken());
        }
    }

    private ApiSeverity escalate(ApiFailureClassifier.Classification classification, int consecutiveFailures) {
        if (classification.severity() == ApiSeverity.CRITICAL) {
            return ApiSeverity.CRITICAL;
        }
        if (classification.failureType() == ApiFailureType.RATE_LIMIT && consecutiveFailures >= RATE_LIMIT_CRITICAL_STREAK) {
            return ApiSeverity.CRITICAL;
        }
        if (classification.failureType() == ApiFailureType.TIMEOUT && consecutiveFailures >= TIMEOUT_CRITICAL_STREAK) {
            return ApiSeverity.CRITICAL;
        }
        return classification.severity();
    }

    private int elapsedMillis(long startNanos) {
        return (int) Math.max(0, (System.nanoTime() - startNanos) / 1_000_000);
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }
}
