package com.example.application.exception;

public class DownstreamServiceUnavailableException extends GatewayException {
    public DownstreamServiceUnavailableException(String serviceName, Throwable cause) {
        super(503, "downstream_service_unavailable: " + serviceName, cause);
    }
}
