package com.example.application.port.out;

import com.example.application.event.SyncAllRequestedEvent;

public interface ConnectorSyncQueue {
    void enqueue(SyncAllRequestedEvent event);
}
