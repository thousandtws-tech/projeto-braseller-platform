package com.example.interfaces.rest;

import com.example.application.service.PluggyConnectService;
import com.example.application.service.TenantAuthorizationService;
import com.example.domain.model.TenantContext;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.security.SecurityRequirement;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;
import java.util.Map;

@Path("/core/open-finance/pluggy")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Open Finance", description = "Integração Open Finance via Pluggy Connect.")
public class PluggyResource {
    @Inject
    PluggyConnectService pluggyConnectService;

    @Inject
    TenantAuthorizationService tenantAuthorizationService;

    @POST
    @Path("/connect-token")
    @Operation(summary = "Criar Connect Token Pluggy", description = "Gera um token server-side para abrir o widget Pluggy Connect sem expor credenciais no navegador.")
    @SecurityRequirement(name = "bearerAuth")
    public Map<String, String> createConnectToken(
            @HeaderParam("Authorization") String authorizationHeader,
            ConnectTokenRequest request) {
        TenantContext context = tenantAuthorizationService.requireWritable(authorizationHeader);
        String accessToken = pluggyConnectService.createConnectToken(
                context.tenantId(),
                request == null ? null : request.clientUserId());
        return Map.of("accessToken", accessToken);
    }

    @POST
    @Path("/webhooks")
    @Operation(summary = "Receber webhook Pluggy", description = "Confirma eventos da Pluggy rapidamente; processamento pesado deve ser assíncrono.")
    public Response webhook(Map<String, Object> event) {
        String eventName = event == null ? null : String.valueOf(event.get("event"));
        String eventId = event == null ? null : String.valueOf(event.get("eventId"));
        return Response.ok(Map.of(
                "received", true,
                "event", eventName == null ? "unknown" : eventName,
                "eventId", eventId == null ? "unknown" : eventId,
                "receivedAt", Instant.now().toString()
        )).build();
    }

    public record ConnectTokenRequest(String clientUserId) {
    }
}
