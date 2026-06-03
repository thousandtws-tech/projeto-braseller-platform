package com.example.infrastructure.tool;

public interface AgentToolExecutor {
    String toolName();
    String description();
    ToolResult execute(String tenantId, String inputJson);
    boolean isEnabled();
}
