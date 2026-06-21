package com.example.application.command;

public record ResetPasswordCommand(String email, String code, String newPassword) {
}
