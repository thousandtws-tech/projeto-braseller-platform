package com.example.application.exception;

public class FeatureNotConfiguredException extends RuntimeException {
    public FeatureNotConfiguredException(String message) {
        super(message);
    }
}
