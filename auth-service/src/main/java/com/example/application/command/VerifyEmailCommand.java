package com.example.application.command;

public record VerifyEmailCommand(String email, String code) {
}
