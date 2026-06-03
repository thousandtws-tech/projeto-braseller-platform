package com.example.application.port.out;

import com.example.domain.model.AgentExecution;
import com.example.domain.model.ExecutionStatus;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

public interface AgentExecutionRepository {
    AgentExecution save(AgentExecution execution);
    Optional<AgentExecution> findById(String id, String tenantId);
    List<AgentExecution> findByAgentId(String agentId, String tenantId, int limit);
    boolean hasRunningExecution(String agentId, String tenantId);
    AgentExecution finish(String id, String tenantId, ExecutionStatus status,
                          int totalActions, int successActions, int failedActions,
                          String summary, String error, Instant finishedAt);
}
