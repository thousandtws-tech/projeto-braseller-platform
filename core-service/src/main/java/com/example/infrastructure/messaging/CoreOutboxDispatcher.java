package com.example.infrastructure.messaging;

import com.example.application.event.NewSaleEvent;
import com.example.application.event.ReportEntryUpsertRequestedEvent;
import com.example.application.event.SyncAllRequestedEvent;
import com.example.application.service.ConnectorService;
import com.example.infrastructure.client.NewSaleNotificationRequest;
import com.example.infrastructure.client.NotificationRestClient;
import com.example.infrastructure.client.ReportEntryIngestRequest;
import com.example.infrastructure.client.ReportingRestClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import jakarta.ws.rs.core.Response;

@ApplicationScoped
@UnlessBuildProfile("test")
public class CoreOutboxDispatcher {
    private static final Logger LOGGER = Logger.getLogger(CoreOutboxDispatcher.class);

    @Inject
    JdbcOutboxEventRepository outboxEventRepository;

    @Inject
    ObjectMapper objectMapper;

    @Inject
    ConnectorService connectorService;

    @Inject
    @RestClient
    NotificationRestClient notificationRestClient;

    @Inject
    @RestClient
    ReportingRestClient reportingRestClient;

    @ConfigProperty(name = "core.internal-token")
    String internalToken;

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
            switch (outboxEvent.eventType()) {
                case "notification.new-sale.v1" -> {
                    NewSaleEvent event = objectMapper.readValue(outboxEvent.payload(), NewSaleEvent.class);
                    notifyNewSale(event);
                }
                case "core.sync-all-requested.v1" -> {
                    SyncAllRequestedEvent event = objectMapper.readValue(outboxEvent.payload(), SyncAllRequestedEvent.class);
                    connectorService.processSyncAll(event);
                }
                case "reporting.report-entry-upsert-requested.v1" -> {
                    ReportEntryUpsertRequestedEvent event = objectMapper.readValue(outboxEvent.payload(), ReportEntryUpsertRequestedEvent.class);
                    ingestReportEntry(event);
                }
                default -> throw new IllegalArgumentException("unsupported_outbox_event_type:" + outboxEvent.eventType());
            }
            outboxEventRepository.markPublished(outboxEvent.id());
        } catch (Exception exception) {
            LOGGER.errorf(exception, "Failed to dispatch outbox event %s of type %s", outboxEvent.id(), outboxEvent.eventType());
            outboxEventRepository.markFailed(outboxEvent.id(), exception.getMessage(), maxAttempts, retryDelaySeconds);
        }
    }

    private void notifyNewSale(NewSaleEvent event) {
        NewSaleNotificationRequest request = new NewSaleNotificationRequest(
                event.eventId(),
                event.eventType(),
                event.occurredAt(),
                event.tenantId(),
                event.recipientEmail(),
                event.marketplace(),
                event.orderId(),
                event.amount()
        );
        try (Response response = notificationRestClient.newSale(internalToken, request)) {
            requireSuccess(response, "notification-service");
        }
    }

    private void ingestReportEntry(ReportEntryUpsertRequestedEvent event) {
        ReportEntryIngestRequest request = new ReportEntryIngestRequest(
                event.tenantId(),
                event.platform(),
                event.orderId(),
                event.saleDate(),
                event.grossValue(),
                event.receivedValue(),
                event.feeValue(),
                event.receivableValue(),
                event.paymentMethod(),
                event.status(),
                event.releaseDate(),
                event.buyerName(),
                event.invoiceNumber()
        );
        try (Response response = reportingRestClient.ingest(internalToken, request)) {
            requireSuccess(response, "reporting-service");
        }
    }

    private void requireSuccess(Response response, String target) {
        int status = response.getStatus();
        if (status < 200 || status >= 300) {
            throw new IllegalStateException(target + "_returned_http_" + status);
        }
    }
}
