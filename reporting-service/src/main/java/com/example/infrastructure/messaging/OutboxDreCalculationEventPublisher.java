package com.example.infrastructure.messaging;

import com.example.application.event.DreCalculationRequestedEvent;
import com.example.application.port.out.DreCalculationEventPublisher;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@UnlessBuildProfile("test")
public class OutboxDreCalculationEventPublisher implements DreCalculationEventPublisher {
    private static final Logger LOGGER = Logger.getLogger(OutboxDreCalculationEventPublisher.class);

    @Inject
    JdbcOutboxEventRepository outboxEventRepository;

    @Override
    public void publish(DreCalculationRequestedEvent event) {
        outboxEventRepository.save(event.eventId(), event.eventType(), event.jobId(), event.tenantId(), event);
        LOGGER.debugf("Saved DRE calculation event %s to internal outbox", event.eventId());
    }
}
