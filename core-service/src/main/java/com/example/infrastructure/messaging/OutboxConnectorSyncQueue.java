package com.example.infrastructure.messaging;

import com.example.application.event.SyncAllRequestedEvent;
import com.example.application.port.out.ConnectorSyncQueue;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@UnlessBuildProfile("test")
public class OutboxConnectorSyncQueue implements ConnectorSyncQueue {
    private static final Logger LOGGER = Logger.getLogger(OutboxConnectorSyncQueue.class);

    @Inject
    JdbcOutboxEventRepository outboxEventRepository;

    @Override
    public void enqueue(SyncAllRequestedEvent event) {
        outboxEventRepository.save(event.eventId(), event.eventType(), event.eventId(), event.tenantId(), event);
        LOGGER.debugf("Saved sync job %s to internal outbox", event.eventId());
    }
}
