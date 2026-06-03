package com.example.application.service;

import com.example.application.command.*;
import com.example.application.exception.*;
import com.example.application.port.out.*;
import com.example.domain.model.*;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@ApplicationScoped
public class AgentService {

    private static final Logger LOG = Logger.getLogger(AgentService.class);

    @Inject
    AgentRepository agentRepository;

    @Inject
    AgentGoalRepository goalRepository;

    @Inject
    AgentExecutionRepository executionRepository;

    @Inject
    AgentMemoryRepository memoryRepository;

    @Inject
    AgentFeedbackRepository feedbackRepository;

    @Inject
    AgentContextRepository contextRepository;

    @Inject
    AgentDecisionEngine decisionEngine;

    public Agent createAgent(CreateAgentCommand cmd) {
        if (cmd.name() == null || cmd.name().isBlank()) {
            throw new ValidationException("agent_name_required");
        }
        if (cmd.agentType() == null || cmd.agentType().isBlank()) {
            throw new ValidationException("agent_type_required");
        }
        Agent agent = new Agent(
                UUID.randomUUID().toString(),
                cmd.tenantId(),
                cmd.name(),
                cmd.description(),
                cmd.agentType(),
                cmd.capabilities() != null ? cmd.capabilities() : "{}",
                AgentStatus.ACTIVE,
                Instant.now(),
                Instant.now()
        );
        Agent saved = agentRepository.save(agent);
        LOG.infof("Agent created: agentId=%s tenantId=%s type=%s", saved.id(), saved.tenantId(), saved.agentType());
        return saved;
    }

    public List<Agent> listAgents(String tenantId) {
        return agentRepository.findByTenantId(tenantId);
    }

    public Agent getAgent(String agentId, String tenantId) {
        return agentRepository.findById(agentId, tenantId)
                .orElseThrow(() -> new AgentNotFoundException(agentId));
    }

    public Agent pauseAgent(PauseAgentCommand cmd) {
        Agent agent = getAgent(cmd.agentId(), cmd.tenantId());
        LOG.infof("Pausing agent: agentId=%s reason=%s", agent.id(), cmd.reason());
        contextRepository.upsert(new AgentContext(
                UUID.randomUUID().toString(),
                cmd.tenantId(), cmd.agentId(),
                ContextType.AGENT, "pause_reason", cmd.reason(), Instant.now()
        ));
        return agentRepository.updateStatus(cmd.agentId(), cmd.tenantId(), AgentStatus.PAUSED);
    }

    public Agent resumeAgent(String agentId, String tenantId) {
        getAgent(agentId, tenantId);
        return agentRepository.updateStatus(agentId, tenantId, AgentStatus.ACTIVE);
    }

    public AgentGoal createGoal(CreateGoalCommand cmd) {
        getAgent(cmd.agentId(), cmd.tenantId());
        if (cmd.title() == null || cmd.title().isBlank()) {
            throw new ValidationException("goal_title_required");
        }
        AgentGoal goal = new AgentGoal(
                UUID.randomUUID().toString(),
                cmd.tenantId(),
                cmd.agentId(),
                cmd.title(),
                cmd.description(),
                cmd.objective() != null ? cmd.objective() : "{}",
                cmd.priority(),
                GoalStatus.PENDING,
                cmd.deadlineEpochSeconds() != null ? Instant.ofEpochSecond(cmd.deadlineEpochSeconds()) : null,
                null,
                Instant.now(),
                Instant.now()
        );
        AgentGoal saved = goalRepository.save(goal);
        LOG.infof("Goal created: goalId=%s agentId=%s tenantId=%s", saved.id(), saved.agentId(), saved.tenantId());
        return saved;
    }

    public List<AgentGoal> listGoals(String agentId, String tenantId) {
        getAgent(agentId, tenantId);
        return goalRepository.findByAgentId(agentId, tenantId);
    }

    public AgentExecution executeAgent(ExecuteAgentCommand cmd) {
        Agent agent = getAgent(cmd.agentId(), cmd.tenantId());
        if (agent.status() != AgentStatus.ACTIVE) {
            throw new ValidationException("agent_not_active");
        }
        if (executionRepository.hasRunningExecution(cmd.agentId(), cmd.tenantId())) {
            throw new AgentAlreadyRunningException(cmd.agentId());
        }
        AgentGoal goal = goalRepository.findById(cmd.goalId(), cmd.tenantId())
                .orElseThrow(() -> new GoalNotFoundException(cmd.goalId()));

        AgentExecution execution = new AgentExecution(
                UUID.randomUUID().toString(),
                cmd.tenantId(), cmd.agentId(), cmd.goalId(),
                cmd.triggeredBy() != null ? cmd.triggeredBy() : "api",
                ExecutionStatus.RUNNING,
                0, 0, 0, null, null,
                Instant.now(), null, Instant.now()
        );
        AgentExecution saved = executionRepository.save(execution);
        goalRepository.updateStatus(goal.id(), cmd.tenantId(), GoalStatus.IN_PROGRESS, null);
        LOG.infof("Execution started: executionId=%s agentId=%s goalId=%s", saved.id(), saved.agentId(), saved.goalId());

        decisionEngine.runAsync(saved, agent, goal);
        return saved;
    }

    public AgentExecution getExecution(String executionId, String tenantId) {
        return executionRepository.findById(executionId, tenantId)
                .orElseThrow(() -> new ExecutionNotFoundException(executionId));
    }

    public List<AgentExecution> listExecutions(String agentId, String tenantId, int limit) {
        getAgent(agentId, tenantId);
        return executionRepository.findByAgentId(agentId, tenantId, limit);
    }

    public List<AgentMemory> getMemory(String agentId, String tenantId, String memoryTypeStr) {
        getAgent(agentId, tenantId);
        MemoryType memoryType = memoryTypeStr != null ? MemoryType.valueOf(memoryTypeStr.toUpperCase()) : null;
        return memoryRepository.findByAgent(agentId, tenantId, memoryType);
    }

    public AgentMemory storeMemory(StoreMemoryCommand cmd) {
        getAgent(cmd.agentId(), cmd.tenantId());
        MemoryType type = MemoryType.valueOf(cmd.memoryType().toUpperCase());
        Instant expiresAt = (type == MemoryType.SHORT_TERM && cmd.ttlSeconds() != null)
                ? Instant.now().plusSeconds(cmd.ttlSeconds()) : null;
        AgentMemory memory = new AgentMemory(
                UUID.randomUUID().toString(),
                cmd.tenantId(), cmd.agentId(),
                type, cmd.memoryKey(), cmd.memoryValue(),
                cmd.ttlSeconds(), Instant.now(), expiresAt
        );
        return memoryRepository.save(memory);
    }

    public AgentFeedback registerFeedback(RegisterFeedbackCommand cmd) {
        getAgent(cmd.agentId(), cmd.tenantId());
        FeedbackType type = FeedbackType.valueOf(cmd.feedbackType().toUpperCase());
        if (cmd.score() < 0 || cmd.score() > 10) {
            throw new ValidationException("score_must_be_0_to_10");
        }
        AgentFeedback feedback = new AgentFeedback(
                UUID.randomUUID().toString(),
                cmd.tenantId(), cmd.agentId(), cmd.executionId(),
                type, cmd.score(), cmd.comment(),
                cmd.metadataJson() != null ? cmd.metadataJson() : "{}",
                Instant.now()
        );
        AgentFeedback saved = feedbackRepository.save(feedback);
        LOG.infof("Feedback registered: agentId=%s executionId=%s type=%s score=%d",
                cmd.agentId(), cmd.executionId(), type, cmd.score());
        return saved;
    }

    public List<AgentFeedback> listFeedback(String agentId, String tenantId, int limit) {
        getAgent(agentId, tenantId);
        return feedbackRepository.findByAgentId(agentId, tenantId, limit);
    }
}
