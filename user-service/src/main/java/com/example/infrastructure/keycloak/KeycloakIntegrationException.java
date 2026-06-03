package com.example.infrastructure.keycloak;

public class KeycloakIntegrationException extends RuntimeException {
    public KeycloakIntegrationException(String message, Throwable cause) {
        super(message, cause);
    }
}
