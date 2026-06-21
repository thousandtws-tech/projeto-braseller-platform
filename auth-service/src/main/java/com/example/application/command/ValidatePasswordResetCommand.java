package com.example.application.command;

public record ValidatePasswordResetCommand(String email, String code) {
}
