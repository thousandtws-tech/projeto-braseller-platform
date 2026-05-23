package com.example.application.exception;

public class UnsupportedGatewayMethodException extends GatewayException {
    public UnsupportedGatewayMethodException(String method) {
        super(405, "unsupported_gateway_method: " + method);
    }
}
