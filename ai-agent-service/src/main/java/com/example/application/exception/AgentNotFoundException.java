package com.example.application.exception;

public class AgentNotFoundException extends RuntimeException {
    public AgentNotFoundException(String agentId) {
        super("agent_not_found: " + agentId);
    }
}
