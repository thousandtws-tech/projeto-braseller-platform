package com.example.infrastructure.tool;

import com.example.infrastructure.client.NotificationServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class SendNotificationTool implements AgentToolExecutor {

    private static final Logger LOG = Logger.getLogger(SendNotificationTool.class);

    @Inject
    NotificationServiceClient notificationClient;

    @Override
    public String toolName() {
        return "send_notification";
    }

    @Override
    public String description() {
        return "Send an in-app or email notification to a tenant user";
    }

    @Override
    public ToolResult execute(String tenantId, String inputJson) {
        try {
            LOG.infof("Executing send_notification: tenantId=%s", tenantId);
            notificationClient.sendEvent(tenantId, inputJson);
            return ToolResult.success("{\"status\":\"notification_sent\"}");
        } catch (Exception ex) {
            LOG.warnf("send_notification failed: %s", ex.getMessage());
            return ToolResult.failure("notification_send_failed: " + ex.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
