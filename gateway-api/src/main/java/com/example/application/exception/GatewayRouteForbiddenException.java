package com.example.application.exception;

public class GatewayRouteForbiddenException extends GatewayException {
    public GatewayRouteForbiddenException(String serviceSegment, String remainingPath) {
        super(403, "gateway_route_forbidden: /api/" + serviceSegment + "/" + remainingPath);
    }
}
