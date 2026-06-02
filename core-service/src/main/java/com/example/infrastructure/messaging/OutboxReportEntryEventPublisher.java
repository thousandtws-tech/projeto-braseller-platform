package com.example.infrastructure.messaging;

import com.example.application.event.ReportEntryUpsertRequestedEvent;
import com.example.application.port.out.ReportEntryEventPublisher;
import io.quarkus.arc.profile.UnlessBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
@UnlessBuildProfile("test")
public class OutboxReportEntryEventPublisher implements ReportEntryEventPublisher {
    private static final Logger LOGGER = Logger.getLogger(OutboxReportEntryEventPublisher.class);

    @Inject
    JdbcOutboxEventRepository outboxEventRepository;

    @Override
    public void publishReportEntryUpsert(ReportEntryUpsertRequestedEvent event) {
        String aggregateId = event.tenantId() + ":" + event.platform() + ":" + event.orderId();
        outboxEventRepository.save(event.eventId(), event.eventType(), aggregateId, event.tenantId(), event);
        LOGGER.debugf("Saved report entry event %s to internal outbox", event.eventId());
    }
}
