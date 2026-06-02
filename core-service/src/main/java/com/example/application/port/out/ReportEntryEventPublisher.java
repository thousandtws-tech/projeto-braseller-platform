package com.example.application.port.out;

import com.example.application.event.ReportEntryUpsertRequestedEvent;

public interface ReportEntryEventPublisher {
    void publishReportEntryUpsert(ReportEntryUpsertRequestedEvent event);
}
