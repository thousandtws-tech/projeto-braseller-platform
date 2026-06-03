package com.example.infrastructure.llm;

public interface LLMProvider {
    String providerName();
    LLMResponse complete(LLMRequest request);
    boolean isAvailable();
}
