package com.example.infrastructure.config;

import com.example.application.port.out.RouteCatalog;
import com.example.domain.model.DownstreamRoute;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.net.URI;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class ConfiguredRouteCatalog implements RouteCatalog {
    private final List<DownstreamRoute> routes;

    @Inject
    public ConfiguredRouteCatalog(
            @ConfigProperty(name = "gateway.services.auth.url") String authServiceUrl,
            @ConfigProperty(name = "gateway.services.user.url") String userServiceUrl,
            @ConfigProperty(name = "gateway.services.core.url") String coreServiceUrl,
            @ConfigProperty(name = "gateway.services.billing.url") String billingServiceUrl,
            @ConfigProperty(name = "gateway.services.notification.url") String notificationServiceUrl,
            @ConfigProperty(name = "gateway.services.reporting.url") String reportingServiceUrl) {
        this.routes = List.of(
                new DownstreamRoute("auth", "auth-service", URI.create(authServiceUrl), "/auth"),
                new DownstreamRoute("users", "user-service", URI.create(userServiceUrl), "/users", List.of("internal")),
                new DownstreamRoute("core", "core-service", URI.create(coreServiceUrl), "/core"),
                new DownstreamRoute("billing", "billing-service", URI.create(billingServiceUrl), "/billing"),
                new DownstreamRoute("notifications", "notification-service", URI.create(notificationServiceUrl), "/notifications", List.of("events")),
                new DownstreamRoute("reports", "reporting-service", URI.create(reportingServiceUrl), "/reports", List.of("internal"))
        );
    }

    @Override
    public Optional<DownstreamRoute> findByPublicSegment(String publicSegment) {
        String normalizedSegment = publicSegment == null ? "" : publicSegment.trim().toLowerCase(Locale.ROOT);
        String resolvedSegment = resolveAlias(normalizedSegment);
        return routes.stream()
                .filter(route -> route.publicSegment().equals(resolvedSegment))
                .findFirst();
    }

    private String resolveAlias(String publicSegment) {
        if ("notification".equals(publicSegment)) {
            return "notifications";
        }
        if ("reporting".equals(publicSegment)) {
            return "reports";
        }
        return publicSegment;
    }

    @Override
    public List<DownstreamRoute> list() {
        return routes;
    }
}
