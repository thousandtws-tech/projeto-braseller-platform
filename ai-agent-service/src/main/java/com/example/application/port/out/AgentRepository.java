package com.example.application.port.out;

import com.example.domain.model.Agent;
import com.example.domain.model.AgentStatus;

import java.util.List;
import java.util.Optional;

public interface AgentRepository {
    Agent save(Agent agent);
    Optional<Agent> findById(String id, String tenantId);
    List<Agent> findByTenantId(String tenantId);
    Agent updateStatus(String id, String tenantId, AgentStatus status);
    void delete(String id, String tenantId);
}
