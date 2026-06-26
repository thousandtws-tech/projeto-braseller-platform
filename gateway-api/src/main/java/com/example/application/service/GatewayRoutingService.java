package com.example.application.service;

import com.example.application.dto.DownstreamRequest;
import com.example.application.dto.GatewayRequest;
import com.example.application.dto.GatewayResponse;
import com.example.application.dto.GatewayRouteView;
import com.example.application.exception.GatewayRouteForbiddenException;
import com.example.application.exception.GatewayRouteNotFoundException;
import com.example.application.exception.UnsupportedGatewayMethodException;
import com.example.application.port.out.DownstreamServiceClient;
import com.example.application.port.out.RouteCatalog;
import com.example.domain.model.DownstreamRoute;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;

import java.util.List;
import java.util.Set;

@ApplicationScoped
public class GatewayRoutingService {
    private static final Set<String> SUPPORTED_METHODS = Set.of("GET", "POST", "PUT", "PATCH", "DELETE");

    private final RouteCatalog routeCatalog;
    private final DownstreamServiceClient downstreamServiceClient;

    @Inject
    public GatewayRoutingService(RouteCatalog routeCatalog, DownstreamServiceClient downstreamServiceClient) {
        this.routeCatalog = routeCatalog;
        this.downstreamServiceClient = downstreamServiceClient;
    }

    public GatewayResponse forward(GatewayRequest request) {
        if (!SUPPORTED_METHODS.contains(request.method())) {
            throw new UnsupportedGatewayMethodException(request.method());
        }

        DownstreamRoute route = routeCatalog.findByPublicSegment(request.serviceSegment())
                .orElseThrow(() -> new GatewayRouteNotFoundException(request.serviceSegment()));

        if (route.blocks(request.remainingPath()) || hasUnsafePathSegment(request.remainingPath())) {
            throw new GatewayRouteForbiddenException(route.publicSegment(), request.remainingPath());
        }

        return downstreamServiceClient.exchange(new DownstreamRequest(
                request.method(),
                route,
                request.remainingPath(),
                request.queryParameters(),
                request.headers(),
                request.body()
        ));
    }

    public List<GatewayRouteView> routes() {
        return routeCatalog.list().stream()
                .map(route -> new GatewayRouteView(
                        route.publicPath(),
                        route.serviceName(),
                        route.downstreamPathPrefix()
                ))
                .toList();
    }

    private boolean hasUnsafePathSegment(String remainingPath) {
        if (remainingPath == null || remainingPath.isBlank()) {
            return false;
        }
        for (String segment : remainingPath.split("/")) {
            if (".".equals(segment) || "..".equals(segment)) {
                return true;
            }
        }
        return false;
    }
}
