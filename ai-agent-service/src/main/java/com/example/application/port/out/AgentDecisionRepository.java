package com.example.application.port.out;

import com.example.domain.model.AgentDecision;

import java.util.List;
import java.util.Optional;

public interface AgentDecisionRepository {
    AgentDecision save(AgentDecision decision);
    Optional<AgentDecision> findById(String id, String tenantId);
    List<AgentDecision> findByExecutionId(String executionId, String tenantId);
    List<AgentDecision> findRecentByAgent(String agentId, String tenantId, int limit);
    AgentDecision updateOutcome(String id, String tenantId, String outcome);
}
