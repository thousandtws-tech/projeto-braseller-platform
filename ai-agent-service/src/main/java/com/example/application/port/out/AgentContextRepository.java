package com.example.application.port.out;

import com.example.domain.model.AgentContext;
import com.example.domain.model.ContextType;

import java.util.List;
import java.util.Optional;

public interface AgentContextRepository {
    AgentContext upsert(AgentContext context);
    Optional<AgentContext> findByKey(String agentId, String tenantId, ContextType type, String key);
    List<AgentContext> findByAgent(String agentId, String tenantId);
    void deleteByKey(String agentId, String tenantId, ContextType type, String key);
}
