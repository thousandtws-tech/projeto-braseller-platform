package com.example.infrastructure.tool;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.util.List;
import java.util.Optional;
import java.util.stream.StreamSupport;

@ApplicationScoped
public class ToolRegistry {

    private static final Logger LOG = Logger.getLogger(ToolRegistry.class);

    @Inject
    Instance<AgentToolExecutor> tools;

    public Optional<AgentToolExecutor> find(String toolName) {
        return StreamSupport.stream(tools.spliterator(), false)
                .filter(t -> t.isEnabled() && t.toolName().equals(toolName))
                .findFirst();
    }

    public List<String> listEnabledToolNames() {
        return StreamSupport.stream(tools.spliterator(), false)
                .filter(AgentToolExecutor::isEnabled)
                .map(AgentToolExecutor::toolName)
                .toList();
    }

    public List<AgentToolExecutor> listEnabled() {
        return StreamSupport.stream(tools.spliterator(), false)
                .filter(AgentToolExecutor::isEnabled)
                .toList();
    }
}
