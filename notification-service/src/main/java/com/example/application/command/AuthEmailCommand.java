package com.example.application.command;

public record AuthEmailCommand(String recipientEmail, String subject, String message, String purpose) {
}
