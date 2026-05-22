package com.example.application.port.out;

public interface PasswordHasher {
    String hash(String password);

    boolean verify(String password, String storedHash);
}
