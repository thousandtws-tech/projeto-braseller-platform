package com.example.infrastructure.messaging;

import com.example.application.command.NewSaleNotificationCommand;
import com.example.application.service.NotificationService;
import io.quarkus.arc.profile.UnlessBuildProfile;
import io.smallrye.common.annotation.Blocking;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.reactive.messaging.Incoming;
import org.jboss.logging.Logger;

@ApplicationScoped
@UnlessBuildProfile("test")
public class NewSaleEventConsumer {
    private static final Logger LOGGER = Logger.getLogger(NewSaleEventConsumer.class);

    @Inject
    NotificationService notificationService;

    @Incoming("new-sale-events-in")
    @Blocking
    public void consume(NewSaleEvent event) {
        notificationService.notifyNewSale(new NewSaleNotificationCommand(
                event.tenantId(),
                event.recipientEmail(),
                event.marketplace(),
                event.orderId(),
                event.amount()
        ));
        LOGGER.infof("Processed new sale event %s for tenant %s", event.eventId(), event.tenantId());
    }
}
