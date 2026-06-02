package com.example.infrastructure.messaging;

public record OutboxEvent(
        String id,
        String eventType,
        String aggregateId,
        String partitionKey,
        String payload,
        int attempts) {
}
