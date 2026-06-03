package com.example.infrastructure.llm;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

@ApplicationScoped
public class NoOpLLMProvider implements LLMProvider {

    private static final Logger LOG = Logger.getLogger(NoOpLLMProvider.class);

    @Override
    public String providerName() {
        return "noop";
    }

    @Override
    public LLMResponse complete(LLMRequest request) {
        LOG.warnf("NoOpLLMProvider called — no LLM provider configured. Prompt length: %d chars",
                request.prompt().length());
        return new LLMResponse("COMPLETE", "noop", 0, 0, 1.0);
    }

    @Override
    public boolean isAvailable() {
        return true;
    }
}
