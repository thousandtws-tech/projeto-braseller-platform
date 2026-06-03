package com.example.infrastructure.tool;

public record ToolResult(boolean success, String outputJson, String error) {

    public static ToolResult success(String outputJson) {
        return new ToolResult(true, outputJson, null);
    }

    public static ToolResult failure(String error) {
        return new ToolResult(false, "{}", error);
    }
}
