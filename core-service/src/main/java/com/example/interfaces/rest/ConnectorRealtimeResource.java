package com.example.interfaces.rest;

import com.example.application.exception.ConnectorValidationException;
import com.example.application.service.ConnectorRealtimeService;
import com.example.application.service.TenantAuthorizationService;
import com.example.domain.model.TenantContext;
import com.example.domain.model.connector.ConnectorRealtimeEvent;
import com.example.infrastructure.security.ConnectorRealtimeTicketService;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.sse.OutboundSseEvent;
import jakarta.ws.rs.sse.Sse;
import org.jboss.resteasy.reactive.RestStreamElementType;

import java.time.Duration;
import java.util.List;

@Path("/core/connectors")
public class ConnectorRealtimeResource {
    @Inject
    TenantAuthorizationService tenantAuthorizationService;

    @Inject
    ConnectorRealtimeService realtimeService;

    @Inject
    ConnectorRealtimeTicketService ticketService;

    @Inject
    Sse sse;

    @POST
    @Path("/realtime-ticket")
    @Produces(MediaType.APPLICATION_JSON)
    public RealtimeTicketResponse ticket(@HeaderParam("Authorization") String authorizationHeader) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        ConnectorRealtimeTicketService.IssuedTicket ticket = ticketService.issue(context);
        return new RealtimeTicketResponse(ticket.ticket(), ticket.expiresAt(), ticket.streamId());
    }

    @GET
    @Path("/events/replay")
    @Produces(MediaType.APPLICATION_JSON)
    public List<ConnectorRealtimeEvent> replay(
            @HeaderParam("Authorization") String authorizationHeader,
            @QueryParam("cursor") Long cursor,
            @QueryParam("limit") Integer limit) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        return realtimeService.replay(
                context.tenantId(),
                cursor == null ? 0 : Math.max(0, cursor),
                limit == null ? 500 : Math.max(1, Math.min(limit, 1000))
        );
    }

    @GET
    @Path("/events")
    @Produces(MediaType.SERVER_SENT_EVENTS)
    @RestStreamElementType(MediaType.APPLICATION_JSON)
    public Multi<OutboundSseEvent> events(
            @HeaderParam("Authorization") String authorizationHeader,
            @HeaderParam("Last-Event-ID") String lastEventId,
            @QueryParam("cursor") Long cursor) {
        TenantContext context = tenantAuthorizationService.requireReadable(authorizationHeader);
        long resolvedCursor = Math.max(cursor == null ? 0 : cursor, parseCursor(lastEventId));

        Multi<OutboundSseEvent> domainEvents = realtimeService.stream(context.tenantId(), resolvedCursor)
                .map(event -> sse.newEventBuilder()
                        .id(Long.toString(event.sequence()))
                        .name(event.eventType())
                        .mediaType(MediaType.APPLICATION_JSON_TYPE)
                        .data(ConnectorRealtimeEvent.class, event)
                        .build());

        Multi<OutboundSseEvent> heartbeats = Multi.createFrom().ticks()
                .every(Duration.ofSeconds(15))
                .map(ignored -> sse.newEventBuilder().comment("keepalive").build());

        return Multi.createBy().merging().streams(domainEvents, heartbeats);
    }

    private long parseCursor(String value) {
        if (value == null || value.isBlank()) {
            return 0;
        }
        try {
            return Math.max(0, Long.parseLong(value.trim()));
        } catch (NumberFormatException exception) {
            throw new ConnectorValidationException("invalid_realtime_cursor");
        }
    }

    public record RealtimeTicketResponse(
            String ticket,
            java.time.Instant expiresAt,
            String streamId) {
    }
}
