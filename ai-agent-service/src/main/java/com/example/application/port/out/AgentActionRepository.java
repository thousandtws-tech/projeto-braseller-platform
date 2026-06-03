package com.example.application.port.out;

import com.example.domain.model.AgentAction;
import com.example.domain.model.ActionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AgentActionRepository {
    AgentAction save(AgentAction action);
    Optional<AgentAction> findById(String id, String tenantId);
    List<AgentAction> findByExecutionId(String executionId, String tenantId);
    AgentAction finish(String id, String tenantId, ActionStatus status,
                       String outputJson, String error, long durationMs, Instant finishedAt);
}
