package com.example.application.port.out;

import java.util.List;

public interface OutboxEventRepository {
    void save(String aggregateType, String aggregateId, String eventType, String payloadJson);
    List<PendingEvent> findPending(int limit);
    void markProcessed(String id);
    void markFailed(String id, String error);

    record PendingEvent(String id, String aggregateType, String aggregateId, String eventType, String payloadJson) {}
}
