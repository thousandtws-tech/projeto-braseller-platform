package com.example.application.command;

public record RequestPasswordResetCommand(String email, String requestIp) {
}
