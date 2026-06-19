package com.example.application.service;

import com.example.application.port.out.ConnectorRealtimeEventStore;
import com.example.domain.model.connector.ConnectorRealtimeEvent;
import com.example.domain.model.connector.ConnectorStatus;
import com.example.domain.model.connector.ConnectorToken;
import io.smallrye.mutiny.Multi;
import io.smallrye.mutiny.infrastructure.Infrastructure;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@ApplicationScoped
public class ConnectorRealtimeService {
    public static final String SYNC_JOB_QUEUED = "connector.sync-job.queued.v1";
    public static final String SYNC_JOB_PROCESSING = "connector.sync-job.processing.v1";
    public static final String SYNC_JOB_COMPLETED = "connector.sync-job.completed.v1";
    public static final String SYNC_JOB_FAILED = "connector.sync-job.failed.v1";
    public static final String CONNECTOR_AUTHENTICATED = "connector.authenticated.v1";
    public static final String CONNECTOR_TOKEN_REFRESHED = "connector.token-refreshed.v1";

    @Inject
    ConnectorRealtimeEventStore eventStore;

    @ConfigProperty(name = "realtime.poll-interval", defaultValue = "750ms")
    Duration pollInterval;

    @ConfigProperty(name = "realtime.batch-size", defaultValue = "500")
    int batchSize;

    public Multi<ConnectorRealtimeEvent> stream(String tenantId, long cursor) {
        AtomicLong currentCursor = new AtomicLong(Math.max(0, cursor));
        Multi<Long> triggers = Multi.createBy().concatenating().streams(
                Multi.createFrom().item(0L),
                Multi.createFrom().ticks().every(pollInterval)
        );

        return triggers.onItem().transformToMultiAndConcatenate(ignored ->
                Multi.createFrom().deferred(() -> {
                            List<ConnectorRealtimeEvent> events =
                                    eventStore.findAfter(tenantId, currentCursor.get(), batchSize);
                            if (!events.isEmpty()) {
                                currentCursor.set(events.get(events.size() - 1).sequence());
                            }
                            return Multi.createFrom().iterable(events);
                        })
                        .runSubscriptionOn(Infrastructure.getDefaultWorkerPool())
        );
    }

    public List<ConnectorRealtimeEvent> replay(String tenantId, long cursor, int limit) {
        return eventStore.findAfter(tenantId, cursor, limit);
    }

    public void connectorAuthenticated(String tenantId, ConnectorToken token) {
        eventStore.append(tenantId, CONNECTOR_AUTHENTICATED, token.platform(), token);
    }

    public void connectorTokenRefreshed(String tenantId, ConnectorToken token) {
        eventStore.append(tenantId, CONNECTOR_TOKEN_REFRESHED, token.platform(), token);
    }

    public void connectorStatusObserved(String tenantId, ConnectorStatus status) {
        eventStore.append(tenantId, "connector.status-observed.v1", status.platform(), status);
    }

    public int cleanOlderThan(Instant cutoff) {
        return eventStore.deleteOlderThan(cutoff);
    }
}
