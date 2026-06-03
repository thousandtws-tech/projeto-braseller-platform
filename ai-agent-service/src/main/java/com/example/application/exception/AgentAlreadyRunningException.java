package com.example.application.exception;

public class AgentAlreadyRunningException extends RuntimeException {
    public AgentAlreadyRunningException(String agentId) {
        super("agent_already_running: " + agentId);
    }
}
