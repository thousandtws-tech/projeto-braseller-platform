package com.example.domain.model;

import java.time.Instant;

public record CodeValidationResult(boolean valid, Instant expiresAt) {
}
