package com.example.infrastructure.monitoring;

import com.example.application.event.ApiIntegrationAlertEvent;
import com.example.domain.enums.ApiFailureType;
import com.example.domain.enums.ApiSeverity;
import com.example.infrastructure.messaging.JdbcOutboxEventRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.UUID;

@ApplicationScoped
public class ApiIntegrationAlertPublisher {
    private static final Logger LOGGER = Logger.getLogger(ApiIntegrationAlertPublisher.class);

    @Inject
    JdbcOutboxEventRepository outboxEventRepository;

    public void publishCriticalAlert(ApiCallContext context, String integrationName, ApiFailureType failureType,
                                      ApiSeverity severity, String impact, String actionTaken) {
        ApiIntegrationAlertEvent event = ApiIntegrationAlertEvent.create(
                UUID.randomUUID().toString(),
                context.tenantId(),
                null,
                integrationName,
                context.endpoint(),
                failureType,
                severity,
                impact,
                actionTaken
        );
        String aggregateId = event.tenantId() + ":" + integrationName + ":" + failureType;
        outboxEventRepository.save(event.eventId(), event.eventType(), aggregateId, event.tenantId(), event);
        LOGGER.warnf("Published critical api integration alert %s for tenant %s integration %s",
                event.eventId(), event.tenantId(), integrationName);
    }
}
