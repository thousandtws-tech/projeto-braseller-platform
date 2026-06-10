package com.example.application.exception;

public class TransientIdentityGatewayException extends IdentityGatewayException {
    public TransientIdentityGatewayException(int status, String message, Throwable cause) {
        super(status, message, cause);
    }
}
