package com.example.application.exception;

public class ExecutionNotFoundException extends RuntimeException {
    public ExecutionNotFoundException(String executionId) {
        super("execution_not_found: " + executionId);
    }
}
