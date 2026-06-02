package com.example.infrastructure.messaging;

import com.example.application.event.ReportEntryUpsertRequestedEvent;
import com.example.application.port.out.ReportEntryEventPublisher;
import io.quarkus.arc.profile.IfBuildProfile;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
@IfBuildProfile("test")
public class TestReportEntryEventPublisher implements ReportEntryEventPublisher {
    @Override
    public void publishReportEntryUpsert(ReportEntryUpsertRequestedEvent event) {
        // Connector tests do not need to call reporting-service.
    }
}
