package com.example.application.command;

public record VerifyEmailCodeCommand(String email, String code) {
}
