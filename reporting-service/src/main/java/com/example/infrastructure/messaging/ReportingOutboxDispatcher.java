package com.example.infrastructure.messaging;

import com.example.application.event.DreCalculationRequestedEvent;
import com.example.application.service.FiscalAccountingService;
import com.example.domain.model.DreCalculationJob;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
@UnlessBuildProfile("test")
public class ReportingOutboxDispatcher {
    private static final Logger LOGGER = Logger.getLogger(ReportingOutboxDispatcher.class);

    @Inject
    JdbcOutboxEventRepository outboxEventRepository;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    FiscalAccountingService fiscalAccountingService;

    @ConfigProperty(name = "messaging.outbox.batch-size", defaultValue = "50")
    int batchSize;

    @ConfigProperty(name = "messaging.outbox.max-attempts", defaultValue = "10")
    int maxAttempts;

    @ConfigProperty(name = "messaging.outbox.retry-delay-seconds", defaultValue = "30")
    long retryDelaySeconds;

    @Scheduled(every = "{messaging.outbox.dispatch-every}", delayed = "5s")
    void dispatchReadyEvents() {
        for (OutboxEvent event : outboxEventRepository.findReady(batchSize)) {
            dispatch(event);
        }
    }

    private void dispatch(OutboxEvent outboxEvent) {
        if (!outboxEventRepository.markPublishing(outboxEvent.id())) {
            return;
        }
        try {
            if (!"reporting.dre-calculation-requested.v1".equals(outboxEvent.eventType())) {
                throw new IllegalArgumentException("unsupported_outbox_event_type:" + outboxEvent.eventType());
            }
            DreCalculationRequestedEvent event = objectMapper.readValue(outboxEvent.payload(), DreCalculationRequestedEvent.class);
            DreCalculationJob job = fiscalAccountingService.processDreCalculation(event);
            if (job == null) {
                LOGGER.infof("Skipped duplicate or in-flight DRE calculation job %s for tenant %s", event.jobId(), event.tenantId());
            }
            outboxEventRepository.markPublished(outboxEvent.id());
        } catch (Exception exception) {
            LOGGER.errorf(exception, "Failed to dispatch outbox event %s of type %s", outboxEvent.id(), outboxEvent.eventType());
            outboxEventRepository.markFailed(outboxEvent.id(), exception.getMessage(), maxAttempts, retryDelaySeconds);
        }
    }
}
