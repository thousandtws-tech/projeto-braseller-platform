package com.example.application.exception;

public class ConnectorNotFoundException extends RuntimeException {
    public ConnectorNotFoundException(String connectorName) {
        super("connector_not_found: " + connectorName);
    }
}
