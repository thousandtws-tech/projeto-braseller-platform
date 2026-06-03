package com.example.infrastructure.llm;

public record LLMResponse(String content, String model, int promptTokens, int completionTokens, double confidence) {}
