package com.example.application.port.out;

public interface RefreshTokenHasher {
    String hash(String refreshToken);
}
