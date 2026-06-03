package com.example.infrastructure.client;

public class ServiceClientException extends RuntimeException {
    private final int statusCode;
    private final String serviceName;

    public ServiceClientException(String serviceName, int statusCode, String message) {
        super(message);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public ServiceClientException(String serviceName, int statusCode, String message, Throwable cause) {
        super(message, cause);
        this.serviceName = serviceName;
        this.statusCode = statusCode;
    }

    public int statusCode() { return statusCode; }
    public String serviceName() { return serviceName; }
}
