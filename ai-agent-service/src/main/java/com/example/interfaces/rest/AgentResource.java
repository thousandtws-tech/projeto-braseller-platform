package com.example.interfaces.rest;

import com.example.application.command.*;
import com.example.application.service.AgentService;
import com.example.application.service.AgentDecisionEngine;
import com.example.application.service.TenantAuthorizationService;
import com.example.domain.model.*;
import com.example.infrastructure.tool.ToolRegistry;
import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Map;

@Path("/ai-agents")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
public class AgentResource {

    private static final Logger LOG = Logger.getLogger(AgentResource.class);

    @Inject
    AgentService agentService;

    @Inject
    AgentDecisionEngine decisionEngine;

    @Inject
    TenantAuthorizationService authorizationService;

    @Inject
    ToolRegistry toolRegistry;

    // ── Status ──────────────────────────────────────────────────────────────

    @GET
    @Operation(summary = "Status do servico", description = "Retorna status do ai-agent-service.")
    @Tag(name = "Agents")
    public Response status() {
        return Response.ok(Map.of(
                "service", "ai-agent-service",
                "status", "UP",
                "tools", toolRegistry.listEnabledToolNames()
        )).build();
    }

    // ── Agents ───────────────────────────────────────────────────────────────

    @POST
    @Path("/tenants/{tenantId}/agents")
    @Operation(summary = "Registrar agente", description = "Cria um novo agente autonomo para o tenant.")
    @Tag(name = "Agents")
    @APIResponse(responseCode = "201", description = "Agente criado")
    @APIResponse(responseCode = "400", description = "Dados invalidos")
    public Response createAgent(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            CreateAgentRequest request) {
        authorizationService.requireAdmin(auth, tenantId);
        Agent agent = agentService.createAgent(new CreateAgentCommand(
                tenantId, request.name(), request.description(), request.agentType(), request.capabilities()
        ));
        LOG.infof("Agent created via API: agentId=%s tenantId=%s", agent.id(), tenantId);
        return Response.status(Response.Status.CREATED).entity(agent).build();
    }

    @GET
    @Path("/tenants/{tenantId}/agents")
    @Operation(summary = "Listar agentes", description = "Lista todos os agentes do tenant.")
    @Tag(name = "Agents")
    public Response listAgents(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId) {
        authorizationService.requireTenant(auth, tenantId);
        List<Agent> agents = agentService.listAgents(tenantId);
        return Response.ok(agents).build();
    }

    @GET
    @Path("/tenants/{tenantId}/agents/{agentId}")
    @Operation(summary = "Consultar agente", description = "Retorna dados de um agente especifico.")
    @Tag(name = "Agents")
    public Response getAgent(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId) {
        authorizationService.requireTenant(auth, tenantId);
        return Response.ok(agentService.getAgent(agentId, tenantId)).build();
    }

    @POST
    @Path("/tenants/{tenantId}/agents/{agentId}/pause")
    @Operation(summary = "Pausar agente", description = "Pausa a execucao do agente.")
    @Tag(name = "Agents")
    public Response pauseAgent(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId,
            PauseAgentRequest request) {
        authorizationService.requireAdmin(auth, tenantId);
        Agent agent = agentService.pauseAgent(new PauseAgentCommand(
                tenantId, agentId, request != null ? request.reason() : null
        ));
        return Response.ok(agent).build();
    }

    @POST
    @Path("/tenants/{tenantId}/agents/{agentId}/resume")
    @Operation(summary = "Retomar agente", description = "Retoma agente pausado.")
    @Tag(name = "Agents")
    public Response resumeAgent(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId) {
        authorizationService.requireAdmin(auth, tenantId);
        Agent agent = agentService.resumeAgent(agentId, tenantId);
        return Response.ok(agent).build();
    }

    // ── Goals ─────────────────────────────────────────────────────────────────

    @POST
    @Path("/tenants/{tenantId}/agents/{agentId}/goals")
    @Operation(summary = "Criar objetivo", description = "Define um novo objetivo para o agente.")
    @Tag(name = "Goals")
    @APIResponse(responseCode = "201", description = "Objetivo criado")
    public Response createGoal(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId,
            CreateGoalRequest request) {
        authorizationService.requireTenant(auth, tenantId);
        AgentGoal goal = agentService.createGoal(new CreateGoalCommand(
                tenantId, agentId, request.title(), request.description(),
                request.objective(), request.priority() != null ? request.priority() : 5,
                request.deadlineEpochSeconds()
        ));
        return Response.status(Response.Status.CREATED).entity(goal).build();
    }

    @GET
    @Path("/tenants/{tenantId}/agents/{agentId}/goals")
    @Operation(summary = "Listar objetivos", description = "Lista todos os objetivos do agente.")
    @Tag(name = "Goals")
    public Response listGoals(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId) {
        authorizationService.requireTenant(auth, tenantId);
        return Response.ok(agentService.listGoals(agentId, tenantId)).build();
    }

    // ── Executions ────────────────────────────────────────────────────────────

    @POST
    @Path("/tenants/{tenantId}/agents/{agentId}/execute")
    @Operation(summary = "Executar agente", description = "Inicia execucao assincrona do agente para um objetivo.")
    @Tag(name = "Executions")
    @APIResponse(responseCode = "202", description = "Execucao iniciada")
    public Response executeAgent(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId,
            ExecuteAgentRequest request) {
        TenantContext ctx = authorizationService.requireTenant(auth, tenantId);
        AgentExecution execution = agentService.executeAgent(new ExecuteAgentCommand(
                tenantId, agentId, request.goalId(), ctx.userId()
        ));
        return Response.accepted(execution).build();
    }

    @GET
    @Path("/tenants/{tenantId}/agents/{agentId}/executions")
    @Operation(summary = "Historico de execucoes", description = "Lista execucoes do agente.")
    @Tag(name = "Executions")
    public Response listExecutions(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        authorizationService.requireTenant(auth, tenantId);
        return Response.ok(agentService.listExecutions(agentId, tenantId, limit)).build();
    }

    @GET
    @Path("/tenants/{tenantId}/executions/{executionId}")
    @Operation(summary = "Status da execucao", description = "Retorna status e detalhes de uma execucao especifica.")
    @Tag(name = "Executions")
    public Response getExecution(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("executionId") String executionId) {
        authorizationService.requireTenant(auth, tenantId);
        return Response.ok(agentService.getExecution(executionId, tenantId)).build();
    }

    // ── Memory ────────────────────────────────────────────────────────────────

    @GET
    @Path("/tenants/{tenantId}/agents/{agentId}/memory")
    @Operation(summary = "Consultar memoria", description = "Retorna memorias do agente (curta ou longa).")
    @Tag(name = "Memory")
    public Response getMemory(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId,
            @QueryParam("type") String memoryType) {
        authorizationService.requireTenant(auth, tenantId);
        return Response.ok(agentService.getMemory(agentId, tenantId, memoryType)).build();
    }

    @POST
    @Path("/tenants/{tenantId}/agents/{agentId}/memory")
    @Operation(summary = "Registrar memoria", description = "Armazena entrada na memoria do agente.")
    @Tag(name = "Memory")
    @APIResponse(responseCode = "201", description = "Memoria armazenada")
    public Response storeMemory(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId,
            StoreMemoryRequest request) {
        authorizationService.requireTenant(auth, tenantId);
        AgentMemory memory = agentService.storeMemory(new StoreMemoryCommand(
                tenantId, agentId, request.memoryType(), request.memoryKey(),
                request.memoryValue(), request.ttlSeconds()
        ));
        return Response.status(Response.Status.CREATED).entity(memory).build();
    }

    // ── Decisions ─────────────────────────────────────────────────────────────

    @GET
    @Path("/tenants/{tenantId}/executions/{executionId}/decisions")
    @Operation(summary = "Decisoes da execucao", description = "Lista decisoes tomadas pelo agente em uma execucao.")
    @Tag(name = "Decisions")
    public Response getDecisions(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("executionId") String executionId) {
        authorizationService.requireTenant(auth, tenantId);
        agentService.getExecution(executionId, tenantId);
        return Response.ok(Map.of("executionId", executionId, "decisions", List.of())).build();
    }

    // ── Actions ───────────────────────────────────────────────────────────────

    @POST
    @Path("/tenants/{tenantId}/agents/{agentId}/actions")
    @Operation(summary = "Executar acao manual", description = "Executa uma ferramenta diretamente sem criar execucao completa.")
    @Tag(name = "Agents")
    @APIResponse(responseCode = "202", description = "Acao iniciada")
    public Response executeManualAction(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId,
            ExecuteManualActionRequest request) {
        authorizationService.requireAdmin(auth, tenantId);
        agentService.getAgent(agentId, tenantId);
        return Response.accepted(Map.of(
                "toolName", request.toolName(),
                "status", "ACCEPTED",
                "message", "Use /execute com um goal para execucoes completas do agente"
        )).build();
    }

    // ── Feedback ──────────────────────────────────────────────────────────────

    @POST
    @Path("/tenants/{tenantId}/agents/{agentId}/feedback")
    @Operation(summary = "Registrar feedback", description = "Registra feedback humano sobre desempenho do agente.")
    @Tag(name = "Feedback")
    @APIResponse(responseCode = "201", description = "Feedback registrado")
    public Response registerFeedback(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId,
            RegisterFeedbackRequest request) {
        authorizationService.requireTenant(auth, tenantId);
        AgentFeedback feedback = agentService.registerFeedback(new RegisterFeedbackCommand(
                tenantId, agentId, request.executionId(),
                request.feedbackType(), request.score(),
                request.comment(), request.metadataJson()
        ));
        return Response.status(Response.Status.CREATED).entity(feedback).build();
    }

    @GET
    @Path("/tenants/{tenantId}/agents/{agentId}/feedback")
    @Operation(summary = "Listar feedback", description = "Lista feedbacks registrados para o agente.")
    @Tag(name = "Feedback")
    public Response listFeedback(
            @Parameter(hidden = true) @HeaderParam("Authorization") String auth,
            @PathParam("tenantId") String tenantId,
            @PathParam("agentId") String agentId,
            @QueryParam("limit") @DefaultValue("20") int limit) {
        authorizationService.requireTenant(auth, tenantId);
        return Response.ok(agentService.listFeedback(agentId, tenantId, limit)).build();
    }

    // ── Request Records ───────────────────────────────────────────────────────

    public record CreateAgentRequest(String name, String description, String agentType, String capabilities) {}
    public record PauseAgentRequest(String reason) {}
    public record CreateGoalRequest(String title, String description, String objective, Integer priority, Long deadlineEpochSeconds) {}
    public record ExecuteAgentRequest(String goalId) {}
    public record StoreMemoryRequest(String memoryType, String memoryKey, String memoryValue, Integer ttlSeconds) {}
    public record ExecuteManualActionRequest(String toolName, String inputJson) {}
    public record RegisterFeedbackRequest(String executionId, String feedbackType, int score, String comment, String metadataJson) {}
}
