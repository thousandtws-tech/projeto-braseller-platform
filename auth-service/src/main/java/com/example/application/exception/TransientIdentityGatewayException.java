package com.example.application.exception;

public class TransientIdentityGatewayException extends IdentityGatewayException {
    public TransientIdentityGatewayException(int status, String message) {
        super(status, message);
    }

    public TransientIdentityGatewayException(int status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
