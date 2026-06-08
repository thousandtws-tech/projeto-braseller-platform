package com.example.application.exception;

public class ConnectorRateLimitException extends ConnectorValidationException {
    public ConnectorRateLimitException(String message) {
        super(message);
    }
}
