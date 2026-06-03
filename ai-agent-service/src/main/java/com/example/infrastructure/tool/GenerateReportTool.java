package com.example.infrastructure.tool;

import com.example.infrastructure.client.ReportingServiceClient;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

@ApplicationScoped
public class GenerateReportTool implements AgentToolExecutor {

    private static final Logger LOG = Logger.getLogger(GenerateReportTool.class);

    @Inject
    ReportingServiceClient reportingClient;

    @Override
    public String toolName() {
        return "generate_report";
    }

    @Override
    public String description() {
        return "Fetch financial summary or dashboard data from reporting-service";
    }

    @Override
    public ToolResult execute(String tenantId, String inputJson) {
        try {
            LOG.infof("Executing generate_report: tenantId=%s", tenantId);
            String summary = reportingClient.getSummary(tenantId);
            return ToolResult.success("{\"report\":" + summary + "}");
        } catch (Exception ex) {
            LOG.warnf("generate_report failed: %s", ex.getMessage());
            return ToolResult.failure("report_generation_failed: " + ex.getMessage());
        }
    }

    @Override
    public boolean isEnabled() {
        return true;
    }
}
