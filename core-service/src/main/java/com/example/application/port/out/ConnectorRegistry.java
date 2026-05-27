package com.example.application.port.out;

import java.util.List;
import java.util.Optional;

public interface ConnectorRegistry {
    Optional<MarketplaceConnector> find(String connectorName);

    List<MarketplaceConnector> list();
}
