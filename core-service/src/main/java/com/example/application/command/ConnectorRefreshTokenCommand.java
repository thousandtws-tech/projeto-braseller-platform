package com.example.application.command;

public record ConnectorRefreshTokenCommand(String connectorName, String tenantId) {
}
