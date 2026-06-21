package com.example.application.service;

import com.example.application.port.out.ApiIntegrationEventRepository;
import com.example.application.port.out.ApiIntegrationStatusRepository;
import com.example.domain.enums.ApiCallOutcome;
import com.example.domain.enums.ApiSeverity;
import com.example.domain.model.monitoring.IntegrationEventLog;
import com.example.domain.model.monitoring.IntegrationHealthSummary;
import com.example.domain.model.monitoring.NewApiIntegrationEvent;
import com.example.infrastructure.monitoring.ApiCallContext;
import com.example.infrastructure.monitoring.ApiIntegrationAlertPublisher;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;

@ApplicationScoped
public class ApiIntegrationMonitoringService {
    private static final int DEFAULT_LOG_LIMIT = 100;

    private final ApiIntegrationEventRepository eventRepository;
    private final ApiIntegrationStatusRepository statusRepository;
    private final ApiIntegrationAlertPublisher alertPublisher;

    @Inject
    public ApiIntegrationMonitoringService(
            ApiIntegrationEventRepository eventRepository,
            ApiIntegrationStatusRepository statusRepository,
            ApiIntegrationAlertPublisher alertPublisher) {
        this.eventRepository = eventRepository;
        this.statusRepository = statusRepository;
        this.alertPublisher = alertPublisher;
    }

    public List<IntegrationHealthSummary> getHealthSummaries(String tenantId) {
        return statusRepository.findHealthSummaries(tenantId);
    }

    public List<IntegrationEventLog> getLogs(String tenantId, String integrationName, ApiSeverity severity, Integer limit) {
        return eventRepository.findLogs(tenantId, integrationName, severity, limit == null ? DEFAULT_LOG_LIMIT : limit);
    }

    public void recordExternalEvent(NewApiIntegrationEvent event) {
        eventRepository.record(event);
        if (event.outcome() == ApiCallOutcome.SUCCESS) {
            statusRepository.applySuccess(event.tenantId(), event.integrationName(),
                    event.responseTimeMs() == null ? 0 : event.responseTimeMs());
            return;
        }
        statusRepository.applyFailure(event.tenantId(), event.integrationName(), event.responseTimeMs(),
                event.failureType(), event.severity());
        if (event.severity() == ApiSeverity.CRITICAL) {
            alertPublisher.publishCriticalAlert(
                    ApiCallContext.of(event.tenantId(), event.integrationName(), event.endpoint(), event.operation()),
                    event.integrationName(), event.failureType(), event.severity(), event.impact(), event.actionTaken());
        }
    }

    @Scheduled(every = "{api-integration-monitoring.aggregation-every}", delayed = "1m")
    void recompute24hWindows() {
        Instant since = Instant.now().minus(24, ChronoUnit.HOURS);
        for (String[] pair : statusRepository.findAllTenantIntegrationPairs()) {
            String tenantId = pair[0];
            String integrationName = pair[1];
            int requests24h = eventRepository.countSince(tenantId, integrationName, since);
            int failures24h = eventRepository.countFailuresSince(tenantId, integrationName, since);
            statusRepository.recompute24hWindow(tenantId, integrationName, requests24h, failures24h, availability(requests24h, failures24h));
        }
    }

    private BigDecimal availability(int requests24h, int failures24h) {
        if (requests24h == 0) {
            return BigDecimal.valueOf(100).setScale(2, RoundingMode.HALF_UP);
        }
        BigDecimal successes = BigDecimal.valueOf(requests24h - failures24h);
        return successes.multiply(BigDecimal.valueOf(100))
                .divide(BigDecimal.valueOf(requests24h), 2, RoundingMode.HALF_UP);
    }
}
