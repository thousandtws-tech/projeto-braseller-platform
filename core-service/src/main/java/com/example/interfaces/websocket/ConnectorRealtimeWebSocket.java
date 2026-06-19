package com.example.interfaces.websocket;

import com.example.application.exception.ConnectorValidationException;
import com.example.application.service.ConnectorRealtimeService;
import com.example.domain.model.connector.ConnectorRealtimeEvent;
import com.example.infrastructure.security.ConnectorRealtimeTicketService;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.smallrye.mutiny.Multi;
import jakarta.inject.Inject;

import java.time.Instant;

@WebSocket(path = "/core/connectors/events/ws/{ticket}/{cursor}")
public class ConnectorRealtimeWebSocket {
    @Inject
    ConnectorRealtimeTicketService ticketService;

    @Inject
    ConnectorRealtimeService realtimeService;

    @OnOpen
    public Multi<ConnectorRealtimeEvent> onOpen(
            @PathParam String ticket,
            @PathParam String cursor) {
        String tenantId = ticketService.requireTenant(ticket);
        return realtimeService.stream(tenantId, parseCursor(cursor));
    }

    @OnTextMessage
    public ControlResponse onMessage(ControlMessage message) {
        String type = message == null || message.type() == null ? "unknown" : message.type();
        return switch (type) {
            case "ping" -> new ControlResponse("pong", message.cursor(), Instant.now());
            case "ack" -> new ControlResponse("acknowledged", message.cursor(), Instant.now());
            default -> new ControlResponse("unsupported", message == null ? null : message.cursor(), Instant.now());
        };
    }

    private long parseCursor(String value) {
        try {
            return Math.max(0, Long.parseLong(value));
        } catch (Exception exception) {
            throw new ConnectorValidationException("invalid_realtime_cursor");
        }
    }

    public record ControlMessage(String type, Long cursor) {
    }

    public record ControlResponse(String type, Long cursor, Instant at) {
    }
}
