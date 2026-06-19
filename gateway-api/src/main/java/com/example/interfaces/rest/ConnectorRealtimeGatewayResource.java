package com.example.interfaces.rest;

import com.example.infrastructure.client.CoreRealtimeRestClient;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.resteasy.reactive.RestStreamElementType;
import org.jboss.resteasy.reactive.client.SseEvent;

@Path("/api/core/connectors")
public class ConnectorRealtimeGatewayResource {
    @Inject
    @RestClient
    CoreRealtimeRestClient coreRealtimeRestClient;

    @Inject
    Sse sse;

    @GET
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<OutboundSseEvent> events(
            @HeaderParam("Authorization") String authorization,
            @HeaderParam("Last-Event-ID") String lastEventId,
            @QueryParam("cursor") Long cursor) {
        long resolvedCursor = cursor == null ? 0 : Math.max(0, cursor);
        String resolvedLastEventId = lastEventId == null || lastEventId.isBlank()
                ? Long.toString(resolvedCursor)
                : lastEventId.trim();

        return coreRealtimeRestClient
                .events(authorization, resolvedLastEventId, resolvedCursor)
                .map(this::toOutboundEvent);
    }

    private OutboundSseEvent toOutboundEvent(SseEvent<String> event) {
        OutboundSseEvent.Builder builder = sse.newEventBuilder();
        if (event.id() != null && !event.id().isBlank()) {
            builder.id(event.id());
        }
        if (event.name() != null && !event.name().isBlank()) {
            builder.name(event.name());
        }
        if (event.comment() != null && !event.comment().isBlank()) {
            builder.comment(event.comment());
        }
        if (event.data() != null && !event.data().isBlank()) {
            builder.mediaType(MediaType.APPLICATION_JSON_TYPE).data(String.class, event.data());
        }
        return builder.build();
    }
}
