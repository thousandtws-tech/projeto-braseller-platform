package com.example.infrastructure.connector;

import com.example.application.port.out.ConnectorRegistry;
import com.example.application.port.out.MarketplaceConnector;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

@ApplicationScoped
public class CdiConnectorRegistry implements ConnectorRegistry {
    @Inject
    Instance<MarketplaceConnector> connectors;

    @Override
    public Optional<MarketplaceConnector> find(String connectorName) {
        String normalizedName = connectorName == null ? "" : connectorName.trim().toLowerCase(Locale.ROOT);
        return list().stream()
                .filter(connector -> connector.name().equals(normalizedName))
                .findFirst();
    }

    @Override
    public List<MarketplaceConnector> list() {
        return connectors.stream()
                .sorted(Comparator.comparing(MarketplaceConnector::name))
                .toList();
    }
}
