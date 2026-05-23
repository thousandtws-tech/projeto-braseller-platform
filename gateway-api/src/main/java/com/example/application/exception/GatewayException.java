package com.example.application.exception;

public class GatewayException extends RuntimeException {
    private final int status;

    public GatewayException(int status, String message) {
        super(message);
        this.status = status;
    }

    public GatewayException(int status, String message, Throwable cause) {
        super(message, cause);
        this.status = status;
    }

    public int status() {
        return status;
    }
}
