package com.example.application.port.out;

import com.example.application.event.DreCalculationRequestedEvent;
import com.example.domain.model.DreCalculationJob;
import com.example.domain.model.DreStatement;

import java.util.Optional;

public interface DreCalculationJobRepository {
    DreCalculationJob createQueued(DreCalculationRequestedEvent event);

    void markProcessing(String jobId);

    boolean tryMarkProcessing(String jobId);

    DreCalculationJob markCompleted(String jobId, DreStatement statement);

    DreCalculationJob markFailed(String jobId, String errorMessage);

    Optional<DreCalculationJob> find(String tenantId, String jobId);
}
