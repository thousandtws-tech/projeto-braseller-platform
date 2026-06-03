package com.example.application.port.out;

import com.example.domain.model.AgentFeedback;

import java.util.List;

public interface AgentFeedbackRepository {
    AgentFeedback save(AgentFeedback feedback);
    List<AgentFeedback> findByAgentId(String agentId, String tenantId, int limit);
    List<AgentFeedback> findByExecutionId(String executionId, String tenantId);
    double averageScoreByAgent(String agentId, String tenantId);
}
