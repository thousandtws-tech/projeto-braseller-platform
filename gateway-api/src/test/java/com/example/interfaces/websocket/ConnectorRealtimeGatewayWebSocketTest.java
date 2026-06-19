package com.example.interfaces.websocket;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

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

    @Test
    void acceptsOnlyConfiguredBrowserOrigins() {
        ConnectorRealtimeGatewayWebSocket endpoint = new ConnectorRealtimeGatewayWebSocket();
        endpoint.allowedOrigins = "https://app.example.com, https://admin.example.com";

        assertTrue(endpoint.originAllowed("https://app.example.com"));
        assertTrue(endpoint.originAllowed("https://admin.example.com"));
        assertFalse(endpoint.originAllowed("https://attacker.example"));
        assertFalse(endpoint.originAllowed(null));
    }
}
