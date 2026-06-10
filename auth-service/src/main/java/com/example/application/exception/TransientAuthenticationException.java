package com.example.application.exception;

public class TransientAuthenticationException extends AuthenticationException {
    public TransientAuthenticationException(String message, Throwable cause) {
        super(message);
        initCause(cause);
    }
}
