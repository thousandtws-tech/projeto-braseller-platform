package com.example.infrastructure.tool;

import com.example.infrastructure.client.UserServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class QueryUserTool implements AgentToolExecutor {

    private static final Logger LOG = Logger.getLogger(QueryUserTool.class);

    @Inject
    UserServiceClient userClient;

    @Override
    public String toolName() {
        return "query_user";
    }

    @Override
    public String description() {
        return "Query tenant members and user profile data from user-service";
    }

    @Override
    public ToolResult execute(String tenantId, String inputJson) {
        try {
            LOG.infof("Executing query_user: tenantId=%s", tenantId);
            String members = userClient.getMembers(tenantId);
            return ToolResult.success("{\"members\":" + members + "}");
        } catch (Exception ex) {
            LOG.warnf("query_user failed: %s", ex.getMessage());
            return ToolResult.failure("user_query_failed: " + ex.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
