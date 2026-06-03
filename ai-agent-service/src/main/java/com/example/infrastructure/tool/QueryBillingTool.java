package com.example.infrastructure.tool;

import com.example.infrastructure.client.BillingServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class QueryBillingTool implements AgentToolExecutor {

    private static final Logger LOG = Logger.getLogger(QueryBillingTool.class);

    @Inject
    BillingServiceClient billingClient;

    @Override
    public String toolName() {
        return "query_billing";
    }

    @Override
    public String description() {
        return "Query subscription and billing information for a tenant";
    }

    @Override
    public ToolResult execute(String tenantId, String inputJson) {
        try {
            LOG.infof("Executing query_billing: tenantId=%s", tenantId);
            String subscription = billingClient.getSubscription(tenantId);
            return ToolResult.success("{\"subscription\":" + subscription + "}");
        } catch (Exception ex) {
            LOG.warnf("query_billing failed: %s", ex.getMessage());
            return ToolResult.failure("billing_query_failed: " + ex.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
