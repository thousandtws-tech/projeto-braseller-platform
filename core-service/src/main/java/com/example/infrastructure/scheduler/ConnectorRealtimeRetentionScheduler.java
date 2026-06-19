package com.example.infrastructure.scheduler;

import com.example.application.service.ConnectorRealtimeService;
import io.quarkus.scheduler.Scheduled;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import java.time.Instant;

@ApplicationScoped
public class ConnectorRealtimeRetentionScheduler {
    private static final Logger LOGGER = Logger.getLogger(ConnectorRealtimeRetentionScheduler.class);

    @Inject
    ConnectorRealtimeService realtimeService;

    @ConfigProperty(name = "realtime.retention-days", defaultValue = "7")
    long retentionDays;

    @Scheduled(every = "{realtime.retention-cleanup-every}", delayed = "30s")
    void cleanExpiredEvents() {
        int deleted = realtimeService.cleanOlderThan(
                Instant.now().minusSeconds(Math.max(1, retentionDays) * 86400)
        );
        if (deleted > 0) {
            LOGGER.infof("Deleted %d expired connector realtime events", deleted);
        }
    }
}
