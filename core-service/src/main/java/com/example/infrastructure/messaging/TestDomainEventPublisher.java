package com.example.infrastructure.messaging;

import com.example.application.event.NewSaleEvent;
import com.example.application.port.out.DomainEventPublisher;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@IfBuildProfile("test")
public class TestDomainEventPublisher implements DomainEventPublisher {
    @Override
    public void publishNewSale(NewSaleEvent event) {
        // No broker is needed for connector contract tests.
    }
}
