package com.example.interfaces.websocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ConnectorRealtimeGatewayWebSocketTest {
    @Test
    void convertsInternalHttpUrlsToWebSocketUrls() {
        assertEquals(
                "wss://core-service.internal.example.test",
                ConnectorRealtimeGatewayWebSocket
                        .toWebSocketBaseUri("https://core-service.internal.example.test")
                        .toString()
        );
        assertEquals(
                "ws://localhost:8081",
                ConnectorRealtimeGatewayWebSocket
                        .toWebSocketBaseUri("http://localhost:8081")
                        .toString()
        );
    }
}
