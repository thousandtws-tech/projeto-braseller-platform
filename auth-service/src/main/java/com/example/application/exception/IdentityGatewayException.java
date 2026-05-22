package com.example.application.exception;

public class IdentityGatewayException extends RuntimeException {
    private final int status;

    public IdentityGatewayException(int status, String message) {
        super(message);
        this.status = status;
    }

    public IdentityGatewayException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
