package com.example.infrastructure.messaging;

import com.example.application.event.SyncAllRequestedEvent;
import com.example.application.port.out.ConnectorSyncQueue;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@IfBuildProfile("test")
public class TestConnectorSyncQueue implements ConnectorSyncQueue {
    @Override
    public void enqueue(SyncAllRequestedEvent event) {
        // Connector tests do not need the background scheduler to process queued sync jobs.
    }
}
