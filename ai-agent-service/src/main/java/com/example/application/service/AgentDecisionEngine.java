package com.example.application.service;

import com.example.application.port.out.*;
import com.example.domain.model.*;
import com.example.infrastructure.llm.LLMProvider;
import com.example.infrastructure.llm.LLMProviderFactory;
import com.example.infrastructure.llm.LLMRequest;
import com.example.infrastructure.llm.LLMResponse;
import com.example.infrastructure.tool.AgentToolExecutor;
import com.example.infrastructure.tool.ToolRegistry;
import com.example.infrastructure.tool.ToolResult;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

@ApplicationScoped
public class AgentDecisionEngine {

    private static final Logger LOG = Logger.getLogger(AgentDecisionEngine.class);
    private static final int MAX_ACTIONS_PER_EXECUTION = 20;

    @Inject
    AgentExecutionRepository executionRepository;

    @Inject
    AgentDecisionRepository decisionRepository;

    @Inject
    AgentActionRepository actionRepository;

    @Inject
    AgentMemoryRepository memoryRepository;

    @Inject
    AgentContextRepository contextRepository;

    @Inject
    AgentGoalRepository goalRepository;

    @Inject
    OutboxEventRepository outboxRepository;

    @Inject
    LLMProviderFactory llmProviderFactory;

    @Inject
    ToolRegistry toolRegistry;

    public void runAsync(AgentExecution execution, Agent agent, AgentGoal goal) {
        CompletableFuture.runAsync(() -> run(execution, agent, goal))
                .exceptionally(ex -> {
                    LOG.errorf(ex, "Async execution failed: executionId=%s", execution.id());
                    failExecution(execution, ex.getMessage());
                    return null;
                });
    }

    public void run(AgentExecution execution, Agent agent, AgentGoal goal) {
        LOG.infof("Decision engine starting: executionId=%s agentId=%s goalId=%s",
                execution.id(), agent.id(), goal.id());

        outboxRepository.save("AgentExecution", execution.id(), "agent.execution.started",
                buildExecutionPayload(execution));

        int actionCount = 0;
        int successCount = 0;
        int failedCount = 0;
        String lastError = null;

        try {
            List<AgentMemory> memories = memoryRepository.findByAgent(agent.id(), agent.tenantId(), null);
            List<AgentContext> contexts = contextRepository.findByAgent(agent.id(), agent.tenantId());
            List<AgentDecision> recentDecisions = decisionRepository.findRecentByAgent(agent.id(), agent.tenantId(), 5);

            String contextSummary = buildContextSummary(agent, goal, memories, contexts, recentDecisions);

            while (actionCount < MAX_ACTIONS_PER_EXECUTION) {
                AgentDecision decision = makeDecision(execution, agent, goal, contextSummary, actionCount);

                if ("COMPLETE".equals(decision.decision()) || "NO_ACTION".equals(decision.decision())) {
                    LOG.infof("Engine decided to finish: executionId=%s decision=%s",
                            execution.id(), decision.decision());
                    break;
                }

                AgentAction action = executeDecision(execution, agent, decision);
                actionCount++;

                if (action.status() == ActionStatus.COMPLETED) {
                    successCount++;
                    decisionRepository.updateOutcome(decision.id(), agent.tenantId(), "SUCCESS");

                    AgentMemory shortTermMemory = new AgentMemory(
                            UUID.randomUUID().toString(),
                            agent.tenantId(), agent.id(),
                            MemoryType.SHORT_TERM,
                            "last_action_" + actionCount,
                            action.outputJson(),
                            3600,
                            Instant.now(),
                            Instant.now().plusSeconds(3600)
                    );
                    memoryRepository.save(shortTermMemory);

                    outboxRepository.save("AgentAction", action.id(), "agent.action.created",
                            buildActionPayload(action));
                } else {
                    failedCount++;
                    lastError = action.error();
                    decisionRepository.updateOutcome(decision.id(), agent.tenantId(), "FAILED: " + action.error());

                    if (shouldAbortOnFailure(action.error())) {
                        LOG.warnf("Aborting execution on critical failure: executionId=%s error=%s",
                                execution.id(), action.error());
                        break;
                    }
                }

                contextSummary = refreshContextSummary(contextSummary, action);
            }

            ExecutionStatus finalStatus = failedCount > 0 && successCount == 0
                    ? ExecutionStatus.FAILED : ExecutionStatus.COMPLETED;

            String summary = String.format("Execution finished: %d total, %d success, %d failed",
                    actionCount, successCount, failedCount);

            executionRepository.finish(execution.id(), execution.tenantId(), finalStatus,
                    actionCount, successCount, failedCount, summary, lastError, Instant.now());

            GoalStatus goalStatus = finalStatus == ExecutionStatus.COMPLETED ? GoalStatus.COMPLETED : GoalStatus.FAILED;
            goalRepository.updateStatus(goal.id(), goal.tenantId(), goalStatus, summary);

            String eventType = finalStatus == ExecutionStatus.COMPLETED
                    ? "agent.execution.finished" : "agent.execution.failed";
            outboxRepository.save("AgentExecution", execution.id(), eventType,
                    buildExecutionPayload(execution));

            LOG.infof("Execution done: executionId=%s status=%s actions=%d success=%d failed=%d",
                    execution.id(), finalStatus, actionCount, successCount, failedCount);

        } catch (Exception ex) {
            LOG.errorf(ex, "Execution error: executionId=%s", execution.id());
            executionRepository.finish(execution.id(), execution.tenantId(), ExecutionStatus.FAILED,
                    actionCount, successCount, failedCount + 1, null, ex.getMessage(), Instant.now());
            goalRepository.updateStatus(goal.id(), goal.tenantId(), GoalStatus.FAILED, ex.getMessage());
            outboxRepository.save("AgentExecution", execution.id(), "agent.execution.failed",
                    buildExecutionPayload(execution));
        }
    }

    private AgentDecision makeDecision(AgentExecution execution, Agent agent,
                                       AgentGoal goal, String contextSummary, int stepNumber) {
        LLMProvider llm = llmProviderFactory.getProvider();
        List<String> availableTools = toolRegistry.listEnabledToolNames();

        String prompt = buildDecisionPrompt(goal, contextSummary, availableTools, stepNumber);
        LLMRequest request = new LLMRequest(prompt, agent.agentType(), 1000);

        LLMResponse llmResponse = llm.complete(request);

        String selectedTool = extractToolFromResponse(llmResponse.content(), availableTools);
        String reasoning = llmResponse.content();
        String decision = selectedTool != null ? "EXECUTE_TOOL" : "COMPLETE";
        double confidence = llmResponse.confidence();

        String toolInput = selectedTool != null
                ? extractToolInputFromResponse(llmResponse.content(), selectedTool) : null;

        AgentDecision agentDecision = new AgentDecision(
                UUID.randomUUID().toString(),
                agent.tenantId(), agent.id(),
                execution.id(), goal.id(),
                contextSummary.substring(0, Math.min(contextSummary.length(), 2000)),
                reasoning.substring(0, Math.min(reasoning.length(), 4000)),
                decision, confidence,
                selectedTool, toolInput != null ? toolInput : "{}",
                null, Instant.now()
        );
        AgentDecision saved = decisionRepository.save(agentDecision);
        outboxRepository.save("AgentDecision", saved.id(), "agent.decision.created",
                buildDecisionPayload(saved));
        return saved;
    }

    private AgentAction executeDecision(AgentExecution execution, Agent agent, AgentDecision decision) {
        AgentToolExecutor tool = toolRegistry.find(decision.selectedTool())
                .orElse(null);

        AgentAction action = new AgentAction(
                UUID.randomUUID().toString(),
                agent.tenantId(), agent.id(),
                execution.id(), null,
                "TOOL_EXECUTION",
                decision.selectedTool(),
                decision.toolInputJson(),
                null, ActionStatus.RUNNING,
                null, 0,
                Instant.now(), null, Instant.now()
        );
        action = actionRepository.save(action);
        long startMs = System.currentTimeMillis();

        if (tool == null) {
            long duration = System.currentTimeMillis() - startMs;
            return actionRepository.finish(action.id(), agent.tenantId(),
                    ActionStatus.FAILED, "{}", "tool_not_found: " + decision.selectedTool(),
                    duration, Instant.now());
        }

        try {
            ToolResult result = tool.execute(agent.tenantId(), decision.toolInputJson());
            long duration = System.currentTimeMillis() - startMs;
            ActionStatus status = result.success() ? ActionStatus.COMPLETED : ActionStatus.FAILED;
            return actionRepository.finish(action.id(), agent.tenantId(),
                    status, result.outputJson(), result.error(), duration, Instant.now());
        } catch (Exception ex) {
            long duration = System.currentTimeMillis() - startMs;
            LOG.warnf("Tool execution failed: tool=%s error=%s", decision.selectedTool(), ex.getMessage());
            return actionRepository.finish(action.id(), agent.tenantId(),
                    ActionStatus.FAILED, "{}", ex.getMessage(), duration, Instant.now());
        }
    }

    @Scheduled(every = "30s")
    void dispatchOutboxEvents() {
        List<OutboxEventRepository.PendingEvent> pending = outboxRepository.findPending(50);
        for (OutboxEventRepository.PendingEvent event : pending) {
            try {
                LOG.debugf("Dispatching outbox event: id=%s type=%s", event.id(), event.eventType());
                outboxRepository.markProcessed(event.id());
            } catch (Exception ex) {
                LOG.warnf("Outbox dispatch failed: id=%s error=%s", event.id(), ex.getMessage());
                outboxRepository.markFailed(event.id(), ex.getMessage());
            }
        }
    }

    @Scheduled(every = "10m")
    void cleanExpiredMemories() {
        LOG.debug("Cleaning expired short-term memories");
        memoryRepository.deleteExpired();
    }

    private void failExecution(AgentExecution execution, String error) {
        try {
            executionRepository.finish(execution.id(), execution.tenantId(),
                    ExecutionStatus.FAILED, 0, 0, 1, null, error, Instant.now());
        } catch (Exception ex) {
            LOG.errorf(ex, "Failed to mark execution as failed: executionId=%s", execution.id());
        }
    }

    private String buildContextSummary(Agent agent, AgentGoal goal,
                                       List<AgentMemory> memories, List<AgentContext> contexts,
                                       List<AgentDecision> recentDecisions) {
        StringBuilder sb = new StringBuilder();
        sb.append("AGENT: ").append(agent.name()).append(" (").append(agent.agentType()).append(")\n");
        sb.append("GOAL: ").append(goal.title()).append("\n");
        sb.append("OBJECTIVE: ").append(goal.objective()).append("\n");
        if (!memories.isEmpty()) {
            sb.append("MEMORIES: ").append(memories.size()).append(" entries\n");
        }
        if (!recentDecisions.isEmpty()) {
            sb.append("RECENT_DECISIONS: ").append(recentDecisions.size()).append(" decisions\n");
        }
        return sb.toString();
    }

    private String refreshContextSummary(String current, AgentAction lastAction) {
        return current + "\nLAST_ACTION: " + lastAction.toolName() + " -> " + lastAction.status();
    }

    private String buildDecisionPrompt(AgentGoal goal, String context,
                                       List<String> tools, int step) {
        return String.format(
                "You are an autonomous agent. Step %d.\nContext:\n%s\nGoal: %s\nObjective: %s\n" +
                "Available tools: %s\nDecide: which tool to call next, or respond COMPLETE if the goal is achieved.",
                step + 1, context, goal.title(), goal.objective(), String.join(", ", tools)
        );
    }

    private String extractToolFromResponse(String response, List<String> tools) {
        for (String tool : tools) {
            if (response.toLowerCase().contains(tool.toLowerCase())) {
                return tool;
            }
        }
        return null;
    }

    private String extractToolInputFromResponse(String response, String toolName) {
        int start = response.indexOf("{");
        int end = response.lastIndexOf("}");
        if (start >= 0 && end > start) {
            return response.substring(start, end + 1);
        }
        return "{}";
    }

    private boolean shouldAbortOnFailure(String error) {
        if (error == null) return false;
        return error.contains("unauthorized") || error.contains("forbidden") || error.contains("critical");
    }

    private String buildExecutionPayload(AgentExecution execution) {
        return String.format("{\"executionId\":\"%s\",\"agentId\":\"%s\",\"tenantId\":\"%s\",\"status\":\"%s\"}",
                execution.id(), execution.agentId(), execution.tenantId(), execution.status());
    }

    private String buildActionPayload(AgentAction action) {
        return String.format("{\"actionId\":\"%s\",\"executionId\":\"%s\",\"tool\":\"%s\",\"status\":\"%s\"}",
                action.id(), action.executionId(), action.toolName(), action.status());
    }

    private String buildDecisionPayload(AgentDecision decision) {
        return String.format("{\"decisionId\":\"%s\",\"executionId\":\"%s\",\"tool\":\"%s\",\"confidence\":%.2f}",
                decision.id(), decision.executionId(), decision.selectedTool(), decision.confidence());
    }
}
