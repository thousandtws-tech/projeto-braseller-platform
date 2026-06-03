package com.example.application.exception;

public class LLMProviderException extends RuntimeException {
    private final int statusCode;

    public LLMProviderException(int statusCode, String message) {
        super(message);
        this.statusCode = statusCode;
    }

    public LLMProviderException(int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int statusCode() {
        return statusCode;
    }
}
