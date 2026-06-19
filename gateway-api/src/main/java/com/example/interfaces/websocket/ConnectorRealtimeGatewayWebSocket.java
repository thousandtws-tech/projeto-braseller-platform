package com.example.interfaces.websocket;

import io.quarkus.websockets.next.BasicWebSocketConnector;
import io.quarkus.websockets.next.CloseReason;
import io.quarkus.websockets.next.HandshakeRequest;
import io.quarkus.websockets.next.OnClose;
import io.quarkus.websockets.next.OnError;
import io.quarkus.websockets.next.OnOpen;
import io.quarkus.websockets.next.OnTextMessage;
import io.quarkus.websockets.next.PathParam;
import io.quarkus.websockets.next.WebSocket;
import io.quarkus.websockets.next.WebSocketClientConnection;
import io.quarkus.websockets.next.WebSocketConnection;
import io.smallrye.mutiny.Uni;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.net.URI;
import java.util.Arrays;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@WebSocket(path = "/api/core/connectors/events/ws/{ticket}/{cursor}")
public class ConnectorRealtimeGatewayWebSocket {
    private static final Logger LOGGER = Logger.getLogger(ConnectorRealtimeGatewayWebSocket.class);
    private static final CloseReason POLICY_VIOLATION = new CloseReason(1008, "realtime_policy_violation");
    private static final CloseReason UPSTREAM_UNAVAILABLE = new CloseReason(1013, "core_realtime_unavailable");

    private final ConcurrentHashMap<String, WebSocketClientConnection> upstreamConnections =
            new ConcurrentHashMap<>();

    @Inject
    @ConfigProperty(name = "gateway.services.core.url")
    String coreServiceUrl;

    @Inject
    @ConfigProperty(name = "gateway.realtime.allowed-origins", defaultValue = "*")
    String allowedOrigins;

    @Inject
    @ConfigProperty(name = "gateway.realtime.max-connections", defaultValue = "5000")
    int maxConnections;

    @Inject
    @ConfigProperty(name = "gateway.realtime.tls-configuration-name", defaultValue = "core-realtime")
    String tlsConfigurationName;

    @OnOpen
    public Uni<Void> onOpen(
            WebSocketConnection downstream,
            HandshakeRequest handshake,
            @PathParam String ticket,
            @PathParam String cursor) {
        if (!originAllowed(handshake.header("Origin")) || upstreamConnections.size() >= maxConnections) {
            return downstream.close(POLICY_VIOLATION);
        }

        BasicWebSocketConnector connector = BasicWebSocketConnector.create()
                .baseUri(toWebSocketBaseUri(coreServiceUrl))
                .path("/core/connectors/events/ws/{ticket}/{cursor}")
                .pathParam("ticket", ticket)
                .pathParam("cursor", cursor)
                .executionModel(BasicWebSocketConnector.ExecutionModel.NON_BLOCKING)
                .onTextMessage((upstream, message) ->
                        downstream.sendText(message).subscribe().with(
                                ignored -> {
                                },
                                failure -> closeQuietly(upstream, CloseReason.INTERNAL_SERVER_ERROR)
                        ))
                .onClose((upstream, reason) -> {
                    upstreamConnections.remove(downstream.id(), upstream);
                    closeQuietly(downstream, reason);
                })
                .onError((upstream, failure) -> {
                    LOGGER.warnf(failure, "Core realtime WebSocket failed for gateway connection %s", downstream.id());
                    upstreamConnections.remove(downstream.id(), upstream);
                    closeQuietly(downstream, UPSTREAM_UNAVAILABLE);
                });

        if ("wss".equalsIgnoreCase(toWebSocketBaseUri(coreServiceUrl).getScheme())) {
            connector.tlsConfigurationName(tlsConfigurationName);
        }

        return connector.connect()
                .invoke(upstream -> upstreamConnections.put(downstream.id(), upstream))
                .onFailure().call(failure -> {
                    LOGGER.warnf(failure, "Could not connect gateway WebSocket %s to core realtime", downstream.id());
                    return downstream.close(UPSTREAM_UNAVAILABLE);
                })
                .replaceWithVoid();
    }

    @OnTextMessage
    public Uni<Void> onMessage(WebSocketConnection downstream, String message) {
        WebSocketClientConnection upstream = upstreamConnections.get(downstream.id());
        if (upstream == null || upstream.isClosed()) {
            return downstream.close(UPSTREAM_UNAVAILABLE);
        }
        return upstream.sendText(message);
    }

    @OnClose
    public Uni<Void> onClose(WebSocketConnection downstream) {
        WebSocketClientConnection upstream = upstreamConnections.remove(downstream.id());
        return upstream == null || upstream.isClosed() ? Uni.createFrom().voidItem() : upstream.close();
    }

    @OnError
    public Uni<Void> onError(WebSocketConnection downstream, Throwable failure) {
        LOGGER.warnf(failure, "Public realtime WebSocket failed for connection %s", downstream.id());
        WebSocketClientConnection upstream = upstreamConnections.remove(downstream.id());
        return upstream == null || upstream.isClosed()
                ? downstream.close(CloseReason.INTERNAL_SERVER_ERROR)
                : upstream.close(CloseReason.INTERNAL_SERVER_ERROR)
                        .eventually(() -> downstream.close(CloseReason.INTERNAL_SERVER_ERROR));
    }

    boolean originAllowed(String origin) {
        Set<String> configuredOrigins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .collect(java.util.stream.Collectors.toUnmodifiableSet());
        return configuredOrigins.contains("*") ||
                (origin != null && configuredOrigins.contains(origin));
    }

    static URI toWebSocketBaseUri(String serviceUrl) {
        URI httpUri = URI.create(serviceUrl);
        String scheme = switch (httpUri.getScheme().toLowerCase()) {
            case "https" -> "wss";
            case "http" -> "ws";
            case "wss", "ws" -> httpUri.getScheme().toLowerCase();
            default -> throw new IllegalArgumentException("unsupported_core_realtime_scheme");
        };
        return URI.create(scheme + "://" + httpUri.getAuthority());
    }

    private void closeQuietly(io.quarkus.websockets.next.Connection connection, CloseReason reason) {
        if (connection != null && connection.isOpen()) {
            connection.close(reason).subscribe().with(
                    ignored -> {
                    },
                    failure -> LOGGER.debugf(failure, "Could not close realtime WebSocket connection %s", connection.id())
            );
        }
    }
}
