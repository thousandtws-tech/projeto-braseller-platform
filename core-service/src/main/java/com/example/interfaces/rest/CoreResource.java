package com.example.interfaces.rest;

import com.example.application.exception.InvalidTokenException;
import com.example.application.service.TenantContextService;
import com.example.domain.model.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/core")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Core", description = "Contexto tenant-aware compartilhado entre modulos.")
public class CoreResource {
    @Inject
    TenantContextService tenantContextService;

    @GET
    @Produces(MediaType.TEXT_PLAIN)
    @Operation(summary = "Status do core-service", description = "Verifica se o core-service esta respondendo.")
    @APIResponse(responseCode = "200", description = "Servico em execucao.")
    public String status() {
        return "Core Service is running";
    }

    @GET
    @Path("/context")
    @Operation(summary = "Resolver contexto do tenant", description = "Valida o JWT emitido pelo auth-service e retorna tenant, usuario, e permissoes.")
    @SecurityRequirement(name = "bearerAuth")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Contexto resolvido.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = TenantContext.class))),
            @APIResponse(responseCode = "401", description = "Token ausente, invalido ou expirado.",
                    content = @Content(mediaType = MediaType.APPLICATION_JSON, schema = @Schema(implementation = RestError.class)))
    })
    public Response context(
            @Parameter(description = "Bearer JWT emitido pelo auth-service.", required = true)
            @HeaderParam("Authorization") String authorizationHeader) {
        try {
            return Response.ok(tenantContextService.resolve(authorizationHeader)).build();
        } catch (InvalidTokenException exception) {
            return Response.status(Response.Status.UNAUTHORIZED)
                    .entity(new RestError(exception.getMessage()))
                    .build();
        }
    }
}
