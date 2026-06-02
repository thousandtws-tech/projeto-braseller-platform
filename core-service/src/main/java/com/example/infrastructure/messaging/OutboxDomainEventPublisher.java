package com.example.infrastructure.messaging;

import com.example.application.event.NewSaleEvent;
import com.example.application.port.out.DomainEventPublisher;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@UnlessBuildProfile("test")
public class OutboxDomainEventPublisher implements DomainEventPublisher {
    private static final Logger LOGGER = Logger.getLogger(OutboxDomainEventPublisher.class);

    @Inject
    JdbcOutboxEventRepository outboxEventRepository;

    @Override
    public void publishNewSale(NewSaleEvent event) {
        String aggregateId = event.tenantId() + ":" + event.marketplace() + ":" + event.orderId();
        outboxEventRepository.save(event.eventId(), event.eventType(), aggregateId, event.tenantId(), event);
        LOGGER.debugf("Saved new sale event %s to internal outbox", event.eventId());
    }
}
