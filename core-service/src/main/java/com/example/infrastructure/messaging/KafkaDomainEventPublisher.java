package com.example.infrastructure.messaging;

import com.example.application.event.NewSaleEvent;
import com.example.application.port.out.DomainEventPublisher;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.smallrye.reactive.messaging.kafka.Record;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Channel;
import org.eclipse.microprofile.reactive.messaging.Emitter;
import org.jboss.logging.Logger;

@ApplicationScoped
@UnlessBuildProfile("test")
public class KafkaDomainEventPublisher implements DomainEventPublisher {
    private static final Logger LOGGER = Logger.getLogger(KafkaDomainEventPublisher.class);

    @Inject
    @Channel("new-sale-events-out")
    Emitter<Record<String, NewSaleEvent>> newSaleEmitter;

    @Override
    public void publishNewSale(NewSaleEvent event) {
        newSaleEmitter.send(Record.of(event.tenantId(), event))
                .exceptionally(exception -> {
                    LOGGER.errorf(exception, "Failed to publish new sale event %s", event.eventId());
                    return null;
                });
    }
}
