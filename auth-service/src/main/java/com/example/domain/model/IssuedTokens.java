package com.example.domain.model;

import java.time.Instant;

public record IssuedTokens(String tokenId, String accessToken, String refreshToken, Instant accessExpiresAt,
                           Instant refreshExpiresAt) {
}
