package com.example.interfaces.rest;

import com.example.application.service.ApiIntegrationMonitoringService;
import com.example.application.service.TenantAuthorizationService;
import com.example.domain.enums.ApiCallOutcome;
import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;
import com.example.domain.model.TenantContext;
import com.example.domain.model.monitoring.IntegrationEventLog;
import com.example.domain.model.monitoring.IntegrationHealthSummary;
import com.example.domain.model.monitoring.NewApiIntegrationEvent;
import com.example.infrastructure.security.ConfiguredInternalServiceAuthorizer;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;
import java.util.List;

@Path("/core/integrations")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Integrations Health", description = "Monitoramento de integridade das integracoes externas (Clausula 2.4).")
public class IntegrationsHealthResource {
    @Inject
    ApiIntegrationMonitoringService monitoringService;

    @Inject
    TenantAuthorizationService tenantAuthorizationService;

    @Inject
    ConfiguredInternalServiceAuthorizer internalServiceAuthorizer;

    @GET
    @Path("/health")
    @Operation(summary = "Resumo de integridade", description = "Retorna o status corrente de cada integracao externa do tenant.")
    @SecurityRequirement(name = "bearerAuth")
    public List<IntegrationHealthSummary> health(@HeaderParam("Authorization") String authorizationHeader) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        return monitoringService.getHealthSummaries(context.tenantId());
    }

    @GET
    @Path("/{integrationName}/logs")
    @Operation(summary = "Log auditavel de uma integracao", description = "Retorna o historico de chamadas registradas para a integracao, com severidade/impacto/providencia.")
    @SecurityRequirement(name = "bearerAuth")
    public List<IntegrationEventLog> logs(
            @HeaderParam("Authorization") String authorizationHeader,
            @PathParam("integrationName") String integrationName,
            @QueryParam("severity") String severity,
            @QueryParam("limit") Integer limit) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        return monitoringService.getLogs(context.tenantId(), integrationName, parseSeverity(severity), limit);
    }

    @POST
    @Path("/events")
    @Operation(summary = "Registrar evento de integracao", description = "Ingestao interna de eventos de chamadas a APIs externas feitas por outros servicos (ex.: consulta de CNPJ, validacao Clicksign).")
    @SecurityRequirement(name = "internalToken")
    @RequestBody(required = true, content = @Content(schema = @Schema(implementation = ApiIntegrationEventRequest.class)))
    public Response recordEvent(@HeaderParam("X-Internal-Token") String internalToken, ApiIntegrationEventRequest request) {
        internalServiceAuthorizer.requireInternal(internalToken);
        monitoringService.recordExternalEvent(new NewApiIntegrationEvent(
                request.tenantId(),
                request.integrationName(),
                request.endpoint(),
                request.operation(),
                request.occurredAt() == null ? Instant.now() : request.occurredAt(),
                request.responseTimeMs(),
                request.httpStatus(),
                request.outcome(),
                request.failureType(),
                request.severity(),
                request.impact(),
                request.actionTaken(),
                request.errorMessage()
        ));
        return Response.accepted().build();
    }

    private ApiSeverity parseSeverity(String severity) {
        if (severity == null || severity.isBlank()) {
            return null;
        }
        return ApiSeverity.valueOf(severity.trim().toUpperCase());
    }

    public record ApiIntegrationEventRequest(
            String tenantId,
            String integrationName,
            String endpoint,
            String operation,
            Instant occurredAt,
            Integer responseTimeMs,
            Integer httpStatus,
            ApiCallOutcome outcome,
            ApiFailureType failureType,
            ApiSeverity severity,
            String impact,
            String actionTaken,
            String errorMessage) {
    }
}
