package com.example.application.exception;

public class GatewayRouteNotFoundException extends GatewayException {
    public GatewayRouteNotFoundException(String serviceSegment) {
        super(404, "unknown_gateway_route: " + serviceSegment);
    }
}
