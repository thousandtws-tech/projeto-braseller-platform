package com.example.application.port.out;

import com.example.domain.model.connector.ConnectorRealtimeEvent;

import java.time.Instant;
import java.util.List;

public interface ConnectorRealtimeEventStore {
    ConnectorRealtimeEvent append(
            String tenantId,
            String eventType,
            String aggregateId,
            Object payload);

    List<ConnectorRealtimeEvent> findAfter(String tenantId, long cursor, int limit);

    int deleteOlderThan(Instant cutoff);
}
