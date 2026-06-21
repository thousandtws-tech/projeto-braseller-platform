package com.example.application.port.out;

import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;
import com.example.domain.model.monitoring.IntegrationHealthSummary;

import java.math.BigDecimal;
import java.util.List;

public interface ApiIntegrationStatusRepository {
    /**
     * Records a successful call: resets the consecutive failure count, marks the
     * integration as UP and updates the rolling average response time.
     */
    void applySuccess(String tenantId, String integrationName, int responseTimeMs);

    /**
     * Records a failed call: increments the consecutive failure count and recomputes
     * current_status based on severity/streak. Returns the consecutive failure count
     * after the increment, used to escalate severity.
     */
    int applyFailure(String tenantId, String integrationName, Integer responseTimeMs,
                      ApiFailureType failureType, ApiSeverity severity);

    List<IntegrationHealthSummary> findHealthSummaries(String tenantId);

    void recompute24hWindow(String tenantId, String integrationName, int requests24h, int failures24h, BigDecimal availabilityPct24h);

    List<String[]> findAllTenantIntegrationPairs();
}
