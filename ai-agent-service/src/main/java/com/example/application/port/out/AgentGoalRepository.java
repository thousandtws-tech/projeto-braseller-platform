package com.example.application.port.out;

import com.example.domain.model.AgentGoal;
import com.example.domain.model.GoalStatus;

import java.util.List;
import java.util.Optional;

public interface AgentGoalRepository {
    AgentGoal save(AgentGoal goal);
    Optional<AgentGoal> findById(String id, String tenantId);
    List<AgentGoal> findByAgentId(String agentId, String tenantId);
    List<AgentGoal> findPendingByTenantId(String tenantId);
    AgentGoal updateStatus(String id, String tenantId, GoalStatus status, String result);
}
