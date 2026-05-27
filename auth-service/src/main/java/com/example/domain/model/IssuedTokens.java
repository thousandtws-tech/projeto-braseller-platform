package com.example.domain.model;

import java.time.Instant;

public record IssuedTokens(String accessToken, Instant accessExpiresAt) {
}
