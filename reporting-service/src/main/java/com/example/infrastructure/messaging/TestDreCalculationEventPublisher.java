package com.example.infrastructure.messaging;

import com.example.application.event.DreCalculationRequestedEvent;
import com.example.application.port.out.DreCalculationEventPublisher;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@IfBuildProfile("test")
public class TestDreCalculationEventPublisher implements DreCalculationEventPublisher {
    @Override
    public void publish(DreCalculationRequestedEvent event) {
        // Tests do not need the background scheduler to process queued DRE requests.
    }
}
