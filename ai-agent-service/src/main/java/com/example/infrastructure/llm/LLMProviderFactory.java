package com.example.infrastructure.llm;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

@ApplicationScoped
public class LLMProviderFactory {

    private static final Logger LOG = Logger.getLogger(LLMProviderFactory.class);

    @ConfigProperty(name = "ai.llm.provider", defaultValue = "noop")
    String configuredProvider;

    @Inject
    OpenAIProvider openAIProvider;

    @Inject
    ClaudeProvider claudeProvider;

    @Inject
    NoOpLLMProvider noOpProvider;

    public LLMProvider getProvider() {
        return switch (configuredProvider.toLowerCase()) {
            case "openai" -> resolveOrFallback(openAIProvider);
            case "claude", "anthropic" -> resolveOrFallback(claudeProvider);
            default -> {
                LOG.warnf("Unknown LLM provider '%s', using noop", configuredProvider);
                yield noOpProvider;
            }
        };
    }

    public LLMProvider getProvider(String name) {
        return switch (name.toLowerCase()) {
            case "openai" -> resolveOrFallback(openAIProvider);
            case "claude", "anthropic" -> resolveOrFallback(claudeProvider);
            default -> noOpProvider;
        };
    }

    private LLMProvider resolveOrFallback(LLMProvider provider) {
        if (provider.isAvailable()) {
            return provider;
        }
        LOG.warnf("Provider %s not available, falling back to noop", provider.providerName());
        return noOpProvider;
    }
}
