package com.example.interfaces.rest;

import com.example.application.service.ConnectorService;
import com.example.infrastructure.connector.mercadolivre.JdbcMercadoLivreTokenRepository;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.time.Instant;
import java.util.Optional;

@Path("/core/webhooks/mercado-livre")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Webhooks", description = "Receptores de notificacoes push de marketplaces.")
public class MercadoLivreWebhookResource {

    @Inject
    ConnectorService connectorService;

    @Inject
    JdbcMercadoLivreTokenRepository tokenRepository;

    @POST
    @Operation(
        summary = "Webhook de notificacoes do Mercado Livre",
        description = "Recebe notificacoes push do Mercado Livre (orders_v2, payments, shipments) " +
                      "e enfileira sincronizacao automatica para o tenant correspondente ao seller_id."
    )
    public Response notification(MercadoLivreNotificationPayload payload) {
        if (payload == null || payload.userId() == null) {
            return Response.ok().build();
        }

        if (!isOrderRelatedTopic(payload.topic())) {
            return Response.ok().build();
        }

        Optional<String> tenantId = tokenRepository.findTenantIdBySellerId(String.valueOf(payload.userId()));
        if (tenantId.isEmpty()) {
            return Response.ok().build();
        }

        // Janela de 1 hora para capturar pedidos cujo pagamento veio depois
        connectorService.requestSyncAll(
                "mercado-livre",
                tenantId.get(),
                null,
                Instant.now().minusSeconds(3600)
        );

        return Response.ok().build();
    }

    private boolean isOrderRelatedTopic(String topic) {
        return topic != null && (
                topic.startsWith("orders") ||
                topic.startsWith("payments") ||
                topic.startsWith("shipments")
        );
    }

    public record MercadoLivreNotificationPayload(
            @JsonProperty("resource") String resource,
            @JsonProperty("user_id") Long userId,
            @JsonProperty("topic") String topic,
            @JsonProperty("application_id") Long applicationId,
            @JsonProperty("attempts") Integer attempts,
            @JsonProperty("sent") String sent,
            @JsonProperty("received") String received
    ) {}
}
