package com.example.application.port.out;

import com.example.domain.enums.ApiSeverity;
import com.example.domain.model.monitoring.IntegrationEventLog;
import com.example.domain.model.monitoring.NewApiIntegrationEvent;

import java.util.List;

public interface ApiIntegrationEventRepository {
    void record(NewApiIntegrationEvent event);

    List<IntegrationEventLog> findLogs(String tenantId, String integrationName, ApiSeverity severity, int limit);

    int countSince(String tenantId, String integrationName, java.time.Instant since);

    int countFailuresSince(String tenantId, String integrationName, java.time.Instant since);
}
