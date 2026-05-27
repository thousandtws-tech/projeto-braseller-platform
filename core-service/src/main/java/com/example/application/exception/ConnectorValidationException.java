package com.example.application.exception;

public class ConnectorValidationException extends RuntimeException {
    public ConnectorValidationException(String message) {
        super(message);
    }
}
