package com.example.application.port.out;

import com.example.application.event.SyncAllRequestedEvent;
import com.example.domain.model.connector.SyncJob;
import com.example.domain.model.connector.SyncResult;

import java.util.Optional;

public interface ConnectorSyncJobRepository {
    SyncJob createQueued(SyncAllRequestedEvent event);

    void markProcessing(String jobId);

    boolean tryMarkProcessing(String jobId);

    SyncJob markCompleted(String jobId, SyncResult result);

    SyncJob markFailed(String jobId, String errorMessage);

    Optional<SyncJob> find(String tenantId, String jobId);
}
