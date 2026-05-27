package com.example.application.command;

import java.util.Map;

public record ConnectorAuthenticationCommand(String connectorName, String tenantId, Map<String, String> credentials) {
    public ConnectorAuthenticationCommand {
        credentials = credentials == null ? Map.of() : Map.copyOf(credentials);
    }
}
