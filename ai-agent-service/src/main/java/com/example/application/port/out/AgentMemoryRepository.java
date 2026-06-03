package com.example.application.port.out;

import com.example.domain.model.AgentMemory;
import com.example.domain.model.MemoryType;

import java.util.List;
import java.util.Optional;

public interface AgentMemoryRepository {
    AgentMemory save(AgentMemory memory);
    Optional<AgentMemory> findByKey(String agentId, String tenantId, String memoryKey);
    List<AgentMemory> findByAgent(String agentId, String tenantId, MemoryType memoryType);
    void deleteExpired();
    void deleteByKey(String agentId, String tenantId, String memoryKey);
    void deleteShortTermByAgent(String agentId, String tenantId);
}
