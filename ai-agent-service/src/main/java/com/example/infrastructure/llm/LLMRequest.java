package com.example.infrastructure.llm;

public record LLMRequest(String prompt, String systemRole, int maxTokens) {}
